package com.blueshift.blueshiftinboxdemo.fragments

import android.os.Bundle
import com.blueshift.inbox.BlueshiftInboxFragment
import java.text.SimpleDateFormat
import java.util.Locale

class InboxWithCustomDateFormat : BlueshiftInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setInboxDateFormatter { date ->
            SimpleDateFormat("dd MMMM yyyy - hh:mm aa", Locale.getDefault()).format(
                date
            )
        }
    }
}