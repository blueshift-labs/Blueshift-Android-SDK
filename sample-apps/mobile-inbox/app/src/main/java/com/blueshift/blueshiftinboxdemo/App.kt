package com.blueshift.blueshiftinboxdemo

import android.app.Application
import com.blueshift.Blueshift
import com.blueshift.BlueshiftLogger
import com.blueshift.model.Configuration

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        initBlueshift()
    }

    private fun initBlueshift() {
        BlueshiftLogger.setLogLevel(BlueshiftLogger.VERBOSE)

        val config = Configuration()
        config.apply {
            apiKey = "EVENT API KEY"
            isPushEnabled = true
            isInboxEnabled = true
            isInAppEnabled = true
        }

        Blueshift.getInstance(this).initialize(config)
    }
}