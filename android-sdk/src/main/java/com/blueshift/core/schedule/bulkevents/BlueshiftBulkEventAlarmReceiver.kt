package com.blueshift.core.schedule.bulkevents

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blueshift.core.BlueshiftEventManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlueshiftBulkEventAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            BlueshiftEventManager.sync()
            pendingResult.finish()
        }
    }
}