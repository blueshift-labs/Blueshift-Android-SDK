package com.blueshift

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test

class BlueshiftEncryptedPreferencesTest {
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BlueshiftEncryptedPreferences.init(context)
    }

    @Test
    fun makeSureSaveAndGetStringWorks() {
        BlueshiftEncryptedPreferences.saveString(key = "key", value = "value")
        val result = BlueshiftEncryptedPreferences.getString(key = "key", null)
        assert(result == "value")
    }
}