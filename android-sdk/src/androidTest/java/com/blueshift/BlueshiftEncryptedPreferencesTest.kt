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
    fun savedValueIsReturnedWhenCorrectKeyIsProvided() {
        BlueshiftEncryptedPreferences.saveString(key = "key", value = "value")
        val result = BlueshiftEncryptedPreferences.getString(key = "key", null)
        assert(result == "value")
    }

    @Test
    fun defaultValueIsReturnedWhenIncorrectKeyIsProvided() {
        BlueshiftEncryptedPreferences.saveString(key = "key", value = "value")
        val result = BlueshiftEncryptedPreferences.getString(key = "wrong_key", "default")
        assert(result == "default")
    }
}