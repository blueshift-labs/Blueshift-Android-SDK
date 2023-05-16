package com.blueshift.blueshiftinboxdemo.fragments

import android.os.Bundle
import androidx.core.content.ContextCompat
import com.blueshift.blueshiftinboxdemo.R
import com.blueshift.inbox.BlueshiftInboxFragment

class InboxWithCustomIndicatorClors : BlueshiftInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val color = context?.let { ContextCompat.getColor(it, R.color.custom_color) }
        if (color != null) {
            setInboxUnreadIndicatorColor(color)
            setInboxRefreshIndicatorColors(color)
        }
    }
}