package com.blueshift.blueshiftinboxdemo.fragments

import android.os.Bundle
import android.util.Log
import com.blueshift.blueshiftinboxdemo.R
import com.blueshift.inbox.BlueshiftInboxEventListener
import com.blueshift.inbox.BlueshiftInboxFragment
import com.blueshift.inbox.BlueshiftInboxMessage

class InboxWithCustomListItemLayout : BlueshiftInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setInboxListItemLayout(R.layout.custom_bsft_inbox_list_item)

        setInboxEventListener(object : BlueshiftInboxEventListener {
            override fun onMessageClick(message: BlueshiftInboxMessage?) {
                Log.d("Inbox", "onMessageClick: " + message?.messageId)
            }

            override fun onMessageDelete(message: BlueshiftInboxMessage?) {
                Log.d("Inbox", "onMessageDelete: " + message?.messageId)
            }
        })
    }
}