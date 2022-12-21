package com.blueshift.inbox;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BlueshiftInboxMessageStatus {
    final String account_uuid;
    final String user_uuid;
    final String message_uuid;
    final Date opened_at;
    final Date created_at;
    final BlueshiftInboxMessage.Status status;

    public BlueshiftInboxMessageStatus(JSONObject jsonObject) {
        this.account_uuid = jsonObject.optString("account_uuid");
        this.user_uuid = jsonObject.optString("user_uuid");
        this.message_uuid = jsonObject.optString("message_uuid");
        Date open;
        try {
            open = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).parse(jsonObject.optString("opened_at"));
        } catch (Exception e) {
            open = null;
        }
        this.opened_at = open;
        Date created;
        try {
            created = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).parse(jsonObject.optString("created_at"));
        } catch (Exception e) {
            created = null;
        }
        this.created_at = created;
        this.status = BlueshiftInboxMessage.Status.fromString(jsonObject.optString("status"));
    }

    public boolean isRead() {
        return BlueshiftInboxMessage.Status.READ.equals(status);
    }
}
