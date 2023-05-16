package com.blueshift.inbox;

import android.app.Activity;

public abstract class BlueshiftInboxListener {
    abstract void onMessageClick(BlueshiftInboxMessage message);

    abstract void onMessageDelete(BlueshiftInboxMessage message);
}
