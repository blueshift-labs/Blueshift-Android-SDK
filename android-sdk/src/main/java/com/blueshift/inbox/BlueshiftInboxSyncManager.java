package com.blueshift.inbox;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.blueshift.BlueshiftExecutor;

import java.util.ArrayList;
import java.util.List;

public class BlueshiftInboxSyncManager {

    private static final int BATCH_SIZE = 30;

    public static void syncMessages(@NonNull final Context context) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(new Runnable() {
            @Override
            public void run() {
                // fetch status from api
                List<BlueshiftInboxMessageStatus> statuses = BlueshiftInboxApiManager.getMessageStatuses(context);

                List<String> statusApiAllMsgIds = new ArrayList<>();
                List<String> statusApiReadMsgIds = new ArrayList<>();
                for (BlueshiftInboxMessageStatus status : statuses) {
                    statusApiAllMsgIds.add(status.message_uuid);
                    if (status.isRead()) statusApiReadMsgIds.add(status.message_uuid);
                }

                List<String> messagesToFetchFromApi;

                // fetch message ids from local db
                List<String> dbMsgIds = BlueshiftInboxStoreSQLite.getInstance(context).getStoredMessageIds();
                if (dbMsgIds.isEmpty()) {
                    // no messages found inside the db. this might be the first time we're using inbox
                    // try to fetch all messages.
                    messagesToFetchFromApi = statusApiAllMsgIds;
                } else {
                    // delete all messages that are not part of the status api result
                    // todo: uncomment below line when api is ready
                    // BlueshiftInboxStoreSQLite.getInstance(context).deleteMessages(statusApiAllMsgIds);

                    // find the messages to fetch by removing the locall messages ids from api response
                    for (String mid : dbMsgIds) {
                        statusApiAllMsgIds.remove(mid);
                    }

                    messagesToFetchFromApi = statusApiAllMsgIds;
                }

                // update local db with read status
                String read = BlueshiftInboxMessage.Status.READ.toString();
                BlueshiftInboxStoreSQLite.getInstance(context).updateStatus(statusApiReadMsgIds, read);

                int count = messagesToFetchFromApi.size();
                if (count > 0) {
                    int start = 0;
                    while (start <= count) {
                        int end = Math.min(start + BATCH_SIZE, count - 1);
                        List<String> batchOfIds = messagesToFetchFromApi.subList(start, end);
                        List<BlueshiftInboxMessage> messages = BlueshiftInboxApiManager.getNewMessages(context, batchOfIds);
                        BlueshiftInboxStoreSQLite.getInstance(context).addMessages(messages);

                        start += (BATCH_SIZE + 1);
                    }
                }

                //todo: remove this line later
                List<BlueshiftInboxMessage> messages = BlueshiftInboxApiManager.getNewMessages(context, new ArrayList<String>());
                BlueshiftInboxStoreSQLite.getInstance(context).addMessages(messages);

                sendBroadcast(context);
            }
        });
    }

    private static void sendBroadcast(Context context) {
        Intent intent = new Intent("com.blueshift.inbox.SYNC_COMPLETE");
        context.sendBroadcast(intent);
    }
}
