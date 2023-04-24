package com.blueshift.inbox;

import androidx.annotation.NonNull;

import com.blueshift.framework.BlueshiftBaseSQLiteModel;
import com.blueshift.inappmessage.InAppMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlueshiftInboxMessage extends BlueshiftBaseSQLiteModel {
    private long id; // ID for the local database
    public String accountId;
    public String userId;
    public String messageId;
    public Date createdAt;
    public Date expiresAt;
    public Date deletedAt;
    public String displayOn;
    public String trigger;
    public String messageType;
    public Scope scope;
    public Status status;
    public JSONObject data;
    public JSONObject campaignAttr;

    BlueshiftInboxMessage() {
    }

    BlueshiftInboxMessage(@NonNull JSONObject jsonObject) {
        createdAt = new Date(jsonObject.optLong("created_at") * 1000);
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

    public InAppMessage getInAppMessage() {
        return InAppMessage.getInstance(data);
    }

    @Override
    protected long getId() {
        return id;
    }

    @Override
    protected void setId(long id) {
        this.id = id;
    }

    public enum Status {
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
        INAPP_ONLY, INBOX_ONLY, INBOX_AND_INAPP;

        @NonNull
        @Override
        public String toString() {
            if (this == INAPP_ONLY) return "inapp";
            if (this == INBOX_ONLY) return "inbox";
            if (this == INBOX_AND_INAPP) return "inbox+inapp";
            return super.toString();
        }

        static Scope fromString(String status) {
            if ("inbox".equals(status)) return INBOX_ONLY;
            if ("inbox+inapp".equals(status)) return INBOX_AND_INAPP;
            // default is inapp
            return INAPP_ONLY;
        }
    }
}
