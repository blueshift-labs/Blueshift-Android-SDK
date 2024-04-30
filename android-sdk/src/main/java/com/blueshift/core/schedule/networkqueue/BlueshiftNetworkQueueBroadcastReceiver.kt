package com.blueshift.core.schedule.networkqueue

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blueshift.core.BlueshiftNetworkQueueManager
import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlueshiftNetworkQueueBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = "android.net.conn.CONNECTIVITY_CHANGE"
        if (action == intent?.action) {
            BlueshiftLogger.d("BlueshiftNetworkQueueBroadcastReceiver detected a network change!")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                if (NetworkUtils.isConnected(context)) {
                    BlueshiftNetworkQueueManager.sync()
                }
                pendingResult.finish()
            }
        }
    }
}