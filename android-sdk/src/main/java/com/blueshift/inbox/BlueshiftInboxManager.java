package com.blueshift.inbox;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;

import java.util.ArrayList;
import java.util.List;

public class BlueshiftInboxManager {

    private static final int BATCH_SIZE = 30;
    private static final String TAG = "InboxSyncManager";

    public static void getMessages(@NonNull Context context, BlueshiftInboxCallback<List<BlueshiftInboxMessage>> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            List<BlueshiftInboxMessage> messages = BlueshiftInboxStoreSQLite.getInstance(context).getInboxMessages();

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> callback.onComplete(messages));
            }
        });
    }

    public static void deleteMessage(@NonNull Context context, BlueshiftInboxMessage message, BlueshiftInboxCallback<Boolean> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            BlueshiftInboxStoreSQLite.getInstance(context).deleteMessage(message);

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> callback.onComplete(true));
            }
        });
    }

    public static void insertMessages(@NonNull Context context, List<BlueshiftInboxMessage> messages, BlueshiftInboxCallback<Boolean> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            BlueshiftInboxStoreSQLite.getInstance(context).insertOrReplace(messages);

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> callback.onComplete(true));
            }
        });
    }

    public static void deleteAllMessages(@NonNull Context context, BlueshiftInboxCallback<Boolean> callback) {
        BlueshiftExecutor.getInstance().runOnWorkerThread(() -> {
            BlueshiftInboxStoreSQLite.getInstance(context).deleteAllMessages();

            if (callback != null) {
                BlueshiftExecutor.getInstance().runOnMainThread(() -> {
                    callback.onComplete(true);
                });
            }
        });
    }

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
