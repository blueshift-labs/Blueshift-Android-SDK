package com.blueshift.blueshiftinboxdemo.fragments

import android.os.Bundle
import com.blueshift.inbox.BlueshiftInboxFragment

class InboxWithCustomSorting : BlueshiftInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setInboxComparator { message1, message2 ->
            message2.status.compareTo(message1.status)
        }
    }
}