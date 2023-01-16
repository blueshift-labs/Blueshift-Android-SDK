package com.blueshift.inbox;

import androidx.annotation.NonNull;

import com.blueshift.framework.BlueshiftBaseSQLiteModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlueshiftInboxMessage extends BlueshiftBaseSQLiteModel {
    private long id; // ID for the local database
    String accountId;
    String userId;
    String messageId;
    Date createdAt;
    Date expiresAt;
    Date deletedAt;
    String displayOn;
    String trigger;
    String messageType;
    Scope scope;
    Status status;
    JSONObject data;
    JSONObject campaignAttr;

    BlueshiftInboxMessage() {
    }

    BlueshiftInboxMessage(@NonNull JSONObject jsonObject) {
        try {
            String format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
            createdAt = sdf.parse(jsonObject.optString("created_at"));
        } catch (Exception e) {
            createdAt = new Date(0);
        }

        accountId = jsonObject.optString("account_uuid", "");
        userId = jsonObject.optString("user_uuid", "");
        messageId = jsonObject.optString("message_uuid", "");
        campaignAttr = jsonObject.optJSONObject("campaign_attr");
        status = Status.fromString(jsonObject.optString("status"));

        data = jsonObject.optJSONObject("data");
        if (data != null) {
            // in-app meta data extraction
            JSONObject inapp = data.optJSONObject("inapp");
            if (inapp != null) {
                trigger = inapp.optString("trigger", "now");
                displayOn = inapp.optString("display_on_android", "");
                scope = Scope.fromString(inapp.optString("scope"));
                messageType = inapp.optString("type");
                expiresAt = new Date(inapp.optLong("expires_at", 0));
            }
        }
    }

    public static List<BlueshiftInboxMessage> fromJsonArray(@NonNull JSONArray jsonArray) {
        List<BlueshiftInboxMessage> messages = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            messages.add(new BlueshiftInboxMessage(jsonArray.optJSONObject(i)));
        }
        return messages;
    }

    @Override
    protected long getId() {
        return id;
    }

    @Override
    protected void setId(long id) {
        this.id = id;
    }

    enum Status {
        READ, UNREAD, UNKNOWN;

        @NonNull
        @Override
        public String toString() {
            if (this == READ) return "read";
            if (this == UNREAD) return "unread";
            return super.toString();
        }

        static Status fromString(String status) {
            if ("read".equals(status)) return READ;
            if ("unread".equals(status)) return UNREAD;
            return UNKNOWN;
        }
    }

    enum Scope {
        INAPP, INBOX, INAPP_AND_INBOX;

        @NonNull
        @Override
        public String toString() {
            if (this == INAPP) return "inapp";
            if (this == INBOX) return "inbox";
            if (this == INAPP_AND_INBOX) return "inapp+inbox";
            return super.toString();
        }

        static Scope fromString(String status) {
            if ("inbox".equals(status)) return INBOX;
            if ("inapp+inbox".equals(status)) return INAPP_AND_INBOX;
            // default is inapp
            return INAPP;
        }
    }
}
