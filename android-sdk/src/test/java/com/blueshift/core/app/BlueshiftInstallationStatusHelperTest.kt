package com.blueshift.core.app

import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import java.io.File

class BlueshiftInstallationStatusHelperTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @Test
    fun getInstallationStatus_returnAppInstallWhenDoingAFreshInstallOrUpdateFromAnAppWithoutBlueshiftSDK() {
        // This happens in two scenarios,
        // 1. When user installs the app for the first time.
        // 2. When user update from an app version that does not have Blueshift SDK integrated to it.
        val helper = BlueshiftInstallationStatusHelper()
        val database = mockk<File>()
        every { database.exists() } returns false
        val status = helper.getInstallationStatus("1.0.0", null, database)
        assert(status == BlueshiftInstallationStatus.APP_INSTALL)
    }

    @Test
    fun getInstallationStatus_returnAppUpdateWhenPreviousAppVersionIsNullAndBlueshiftDatabaseExists() {
        // This case happens when someone updates from an app that has Blueshift SDK version older
        // than 3.4.6.
        val helper = BlueshiftInstallationStatusHelper()
        val database = mockk<File>()
        every { database.exists() } returns true
        val status = helper.getInstallationStatus("1.0.0", null, database)
        assert(status == BlueshiftInstallationStatus.APP_UPDATE)
    }

    @Test
    fun getInstallationStatus_returnAppUpdateWhenAppVersionAndPreviousAppVersionDoesNotMatch() {
        val helper = BlueshiftInstallationStatusHelper()
        val database = mockk<File>()
        val status = helper.getInstallationStatus("1.0.0", "2.0.0", database)
        assert(status == BlueshiftInstallationStatus.APP_UPDATE)
    }

    @Test
    fun getInstallationStatus_returnNoChangeWhenAppVersionAndPreviousAppVersionMatches() {
        val helper = BlueshiftInstallationStatusHelper()
        val database = mockk<File>()
        val status = helper.getInstallationStatus("1.0.0", "1.0.0", database)
        assert(status == BlueshiftInstallationStatus.NO_CHANGE)
    }
}