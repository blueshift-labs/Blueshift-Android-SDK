package com.blueshift.blueshiftinboxdemo.fragments

import android.os.Bundle
import com.blueshift.inbox.BlueshiftInboxFragment
import com.blueshift.inbox.BlueshiftInboxMessage

class InboxWithCustomFilter : BlueshiftInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setInboxFilter { message ->
            message.status == BlueshiftInboxMessage.Status.UNREAD
        }
    }
}