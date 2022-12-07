package com.blueshift.inbox;

public class BlueshiftInboxMessageStatus {
    final String account_uuid;
    final String user_uuid;
    final String message_uuid;
    final String opened_at;
    final String created_at;
    final String status;

    public BlueshiftInboxMessageStatus(String account_uuid, String user_uuid, String message_uuid, String opened_at, String created_at, String status) {
        this.account_uuid = account_uuid;
        this.user_uuid = user_uuid;
        this.message_uuid = message_uuid;
        this.opened_at = opened_at;
        this.created_at = created_at;
        this.status = status;
    }
}
