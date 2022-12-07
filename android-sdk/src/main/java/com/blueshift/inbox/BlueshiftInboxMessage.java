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
    Date displayedAt;
    String displayOn;
    String trigger;
    String messageType;
    Status status;
    JSONObject data;
    JSONObject campaignAttr;

    BlueshiftInboxMessage() {
    }

    BlueshiftInboxMessage(@NonNull JSONObject jsonObject) {
        accountId = jsonObject.optString("account_uuid");
        userId = jsonObject.optString("user_uuid");
        messageId = jsonObject.optString("message_uuid");
        Date date;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).parse(jsonObject.optString("created_at"));
        } catch (Exception e) {
            date = new Date();
        }
        createdAt = date;
        status = Status.fromString(jsonObject.optString("status"));
        data = jsonObject.optJSONObject("data");
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
}
