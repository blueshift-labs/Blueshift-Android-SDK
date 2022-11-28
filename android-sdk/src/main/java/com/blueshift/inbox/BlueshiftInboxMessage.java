package com.blueshift.inbox;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlueshiftInboxMessage {
    final String mAccountUUID;
    final String mUserUUID;
    final String mMessageUUID;
    final Date mCreatedAt;
    final boolean mIsRead;
    final JSONObject mData;

    BlueshiftInboxMessage(@NonNull JSONObject jsonObject) {
        mAccountUUID = jsonObject.optString("account_uuid");
        mUserUUID = jsonObject.optString("user_uuid");
        mMessageUUID = jsonObject.optString("message_uuid");
        Date createdAt;
        try {
            createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).parse(jsonObject.optString("created_at"));
        } catch (Exception e) {
            createdAt = new Date();
        }
        mCreatedAt = createdAt;
        mData = jsonObject.optJSONObject("data");
        mIsRead = jsonObject.optBoolean("is_read");
    }

    public static List<BlueshiftInboxMessage> fromJsonArray(@NonNull JSONArray jsonArray) {
        List<BlueshiftInboxMessage> messages = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            messages.add(new BlueshiftInboxMessage(jsonArray.optJSONObject(i)));
        }
        return messages;
    }
}
