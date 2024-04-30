package com.blueshift.core.common

import android.util.Log

object BlueshiftLogger {
    private const val TAG = "Blueshift"
    var enabled = true

    fun d(message: String) {
        if (enabled) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String) {
        if (enabled) {
            Log.e(TAG, message)
        }
    }
}