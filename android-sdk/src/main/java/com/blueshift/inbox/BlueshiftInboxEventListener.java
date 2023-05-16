package com.blueshift.inbox;

import androidx.annotation.MainThread;

public interface BlueshiftInboxEventListener {
    @MainThread
    void onMessageClick(BlueshiftInboxMessage message);

    @MainThread
    void onMessageDelete(BlueshiftInboxMessage message);
}
