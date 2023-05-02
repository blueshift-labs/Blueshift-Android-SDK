package com.blueshift.inbox;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
import com.blueshift.inappmessage.InAppManager;
import com.blueshift.inappmessage.InAppMessage;

import java.util.ArrayList;
import java.util.List;

public class BlueshiftInboxManager {

    private static final int BATCH_SIZE = 30;
    private static final String TAG = "InboxSyncManager";

    /**
     * Get the list of messages stored in the device's storage.
     * <p>
     * Please note that you must call the method {@link BlueshiftInboxManager#syncMessages(Context, BlueshiftInboxCallback)} to get the
     * device's storage updated with latest messages pulled from Blueshift's API.
     *
     * @param context  Valid {@link Context} object
     * @param callback Callback to retrieve the list of messages (on main thread)
     * @noinspection unused
     */
    public static void getMessages(@NonNull Context context, BlueshiftInboxCallback<List<BlueshiftInboxMessage>> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            List<BlueshiftInboxMessage> messages = BlueshiftInboxStoreSQLite.getInstance(context).getInboxMessages();

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> callback.onComplete(messages));
            }
        });
    }

    /**
     * Deletes a message from server and device
     *
     * @param context  Valid {@link Context} object
     * @param message  Valid {@link BlueshiftInboxMessage} object
     * @param callback Callback to retrieve the status of the delete action (on main thread)
     * @noinspection unused
     */
    public static void deleteMessage(@NonNull Context context, BlueshiftInboxMessage message, BlueshiftInboxCallback<Boolean> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            List<String> ids = new ArrayList<>();
            ids.add(message.messageId);

            boolean status = BlueshiftInboxApiManager.deleteMessages(context, ids);
            if (status) {
                BlueshiftInboxStoreSQLite.getInstance(context).deleteMessage(message);
                BlueshiftLogger.d(TAG, "Deleted the message with id = " + message.messageId);
            } else {
                BlueshiftLogger.d(TAG, "Could not delete the message with id = " + message.messageId);
            }

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> callback.onComplete(status));
            }
        });
    }

    /**
     * Wipes all inbox messages from the device's storage. NO message from the server will be deleted.
     * <p>
     * One common use case for this method would be to clean up local data when user logs out.
     *
     * @param context  Valid {@link Context} object
     * @param callback Callback to retrieve the status of the delete action (on main thread)
     * @noinspection unused
     */
    public static void deleteAllMessagesFromDevice(@NonNull Context context, BlueshiftInboxCallback<Boolean> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            BlueshiftInboxStoreSQLite.getInstance(context).deleteAllMessages();

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> {
                    callback.onComplete(true);
                });
            }
        });
    }

    /**
     * Inserts a list of messages into the device's storage.
     *
     * @param context  Valid {@link Context} object
     * @param messages Valid {@link BlueshiftInboxMessage} objects
     * @param callback Callback to retrieve the status of the insert action (on main thread)
     * @noinspection unused
     */
    public static void insertMessages(@NonNull Context context, List<BlueshiftInboxMessage> messages, BlueshiftInboxCallback<Boolean> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            BlueshiftInboxStoreSQLite.getInstance(context).insertOrReplace(messages);

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> callback.onComplete(true));
            }
        });
    }

    /**
     * Gets you the number of unread messages in the device's storage
     *
     * @param context  Valid {@link Context} object
     * @param callback Callback to retrieve the count (on main thread)
     * @noinspection unused
     */
    public static void getUnreadMessagesCount(@NonNull Context context, BlueshiftInboxCallback<Integer> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            int count = BlueshiftInboxStoreSQLite.getInstance(context).getUnreadMessageCount();

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> {
                    callback.onComplete(count);
                });
            }
        });
    }

    /**
     * Syncs the messages in server and their status with the messages stored in the device's storage.
     * <p>
     * Once complete, a callback will be provided with the status of the action (on main thread).
     * A broadcast message will also be sent to notify any consumer about the change in data.
     *
     * @param context  Valid {@link Context} object
     * @param callback Callback to retrieve the status of the sync action (on main thread)
     */
    public static void syncMessages(@NonNull Context context, BlueshiftInboxCallback<Boolean> callback) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(() -> {
            // fetch status from api
            List<BlueshiftInboxMessageStatus> statuses = BlueshiftInboxApiManager.getMessageStatuses(context);
            if (statuses == null) {
                // NULL indicates API error or internet unavailability.
                invokeSyncComplete(context, false, callback);
                return;
            }

            List<String> statusApiAllMsgIds = new ArrayList<>();
            List<String> statusApiReadMsgIds = new ArrayList<>();
            for (BlueshiftInboxMessageStatus status : statuses) {
                statusApiAllMsgIds.add(status.message_uuid);
                if (status.isRead()) statusApiReadMsgIds.add(status.message_uuid);
            }

            printLogs("statusApiAllMsgIds", statusApiAllMsgIds);
            printLogs("statusApiReadMsgIds", statusApiReadMsgIds);

            List<String> messagesToFetchFromApi;

            // fetch message ids from local db
            List<String> dbMsgIds = BlueshiftInboxStoreSQLite.getInstance(context).getStoredMessageIds();
            printLogs("dbMsgIds", dbMsgIds);

            boolean dataChanged = false;

            if (dbMsgIds.isEmpty()) {
                // no messages found inside the db. this might be the first time we're using inbox
                // try to fetch all messages.
                messagesToFetchFromApi = statusApiAllMsgIds;
            } else {
                // delete all messages that are not part of the status api result
                int deletedCount = BlueshiftInboxStoreSQLite.getInstance(context).deleteMessagesExcept(statusApiAllMsgIds);
                if (deletedCount > 0) dataChanged = true;

                // find the messages to fetch by removing the local messages ids from api response
                for (String mid : dbMsgIds) {
                    statusApiAllMsgIds.remove(mid);
                }

                messagesToFetchFromApi = statusApiAllMsgIds;
            }

            // update local db with read status
            String read = BlueshiftInboxMessage.Status.READ.toString();
            int updateCount = BlueshiftInboxStoreSQLite.getInstance(context).updateStatus(statusApiReadMsgIds, read);
            if (updateCount > 0) dataChanged = true;

            printLogs("messagesToFetchFromApi", messagesToFetchFromApi);

            int count = messagesToFetchFromApi.size();
            if (count > 0) {
                int start = 0;
                while (start <= count) {
                    int end = Math.min(start + BATCH_SIZE, count);
                    List<String> batchOfIds = messagesToFetchFromApi.subList(start, end);
                    printLogs("batchOfIds", batchOfIds);
                    List<BlueshiftInboxMessage> messages = BlueshiftInboxApiManager.getNewMessages(context, batchOfIds);
                    BlueshiftInboxStoreSQLite.getInstance(context).addMessages(messages);

                    start += (BATCH_SIZE + 1);
                }

                dataChanged = true;
            }

            invokeSyncComplete(context, dataChanged, callback);
        });
    }

    /**
     * Display a given inbox message to the user.
     *
     * @param message Valid {@link BlueshiftInboxMessage} object
     * @noinspection unused
     */
    public static void displayInboxMessage(@NonNull BlueshiftInboxMessage message) {
        InAppMessage inAppMessage = InAppMessage.getInstance(message.data);
        if (inAppMessage != null) {
            InAppManager.displayInAppMessage(inAppMessage);
        } else {
            BlueshiftLogger.d(TAG, "The given message can not be displayed to the user.");
        }
    }

    private static void invokeSyncComplete(Context context, boolean dataChanged, BlueshiftInboxCallback<Boolean> callback) {
        if (context != null) {
            Intent broadcastIntent = new Intent(BlueshiftConstants.INBOX_SYNC_COMPLETE);
            broadcastIntent.putExtra(BlueshiftConstants.INBOX_DATA_CHANGED, dataChanged);
            context.sendBroadcast(broadcastIntent);

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> callback.onComplete(dataChanged));
            }
        }
    }

    private static void printLogs(String name, List<String> values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String val : values) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }

            builder.append(val);
        }

        BlueshiftLogger.d(TAG, name + " : [" + builder + "]");
    }
}
