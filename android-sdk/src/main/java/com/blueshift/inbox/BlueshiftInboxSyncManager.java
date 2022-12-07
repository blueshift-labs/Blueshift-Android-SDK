package com.blueshift.inbox;

import android.content.Context;

import androidx.annotation.NonNull;

import com.blueshift.BlueshiftExecutor;

import java.util.ArrayList;
import java.util.List;

public class BlueshiftInboxSyncManager {

    public static void syncMessages(@NonNull final Context context) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(new Runnable() {
            @Override
            public void run() {
                // fetch status
                List<BlueshiftInboxMessageStatus> statuses = BlueshiftInboxApiManager.getMessageStatuses(context);
                List<String> allMsgIds = new ArrayList<>();
                List<String> viewedMsgIds = new ArrayList<>();
                for (BlueshiftInboxMessageStatus status : statuses) {
//            if (status.)
                }

                // update local db

                // fetch new content
                List<String> messageIds = new ArrayList<>();
                List<BlueshiftInboxMessage> messages = BlueshiftInboxApiManager.getNewMessages(context, messageIds);
                BlueshiftInboxStoreSQLite.getInstance(context).addMessages(messages);
            }
        });
    }
}
