package com.blueshift.inbox;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;

import java.util.ArrayList;
import java.util.List;

public class BlueshiftInboxSyncManager {

    private static final int BATCH_SIZE = 30;
    private static final String TAG = "InboxSyncManager";

    public static void syncMessages(@NonNull final Context context) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(() -> {
            // fetch status from api
            List<BlueshiftInboxMessageStatus> statuses = BlueshiftInboxApiManager.getMessageStatuses(context);

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

            if (dbMsgIds.isEmpty()) {
                // no messages found inside the db. this might be the first time we're using inbox
                // try to fetch all messages.
                messagesToFetchFromApi = statusApiAllMsgIds;
            } else {
                // delete all messages that are not part of the status api result
                BlueshiftInboxStoreSQLite.getInstance(context).deleteMessagesExcept(statusApiAllMsgIds);

                // find the messages to fetch by removing the locall messages ids from api response
                for (String mid : dbMsgIds) {
                    statusApiAllMsgIds.remove(mid);
                }

                messagesToFetchFromApi = statusApiAllMsgIds;
            }

            // update local db with read status
            String read = BlueshiftInboxMessage.Status.READ.toString();
            BlueshiftInboxStoreSQLite.getInstance(context).updateStatus(statusApiReadMsgIds, read);

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

                notifyDataChanged(context);
            }

            notifySyncComplete(context);
        });
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


    private static void sendBroadcast(Context context, String action) {
        Intent intent = new Intent(action);
        context.sendBroadcast(intent);
    }

    public static void notifySyncComplete(Context context) {
        sendBroadcast(context, BlueshiftConstants.INBOX_SYNC_COMPLETE);
    }

    public static void notifyDataChanged(Context context) {
        sendBroadcast(context, BlueshiftConstants.INBOX_DATA_CHANGED);
    }
}
