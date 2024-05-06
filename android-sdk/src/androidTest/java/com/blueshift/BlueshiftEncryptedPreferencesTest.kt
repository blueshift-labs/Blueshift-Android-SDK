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
        BlueshiftEncryptedPreferences.putString(key = "key", value = "value")
        val result = BlueshiftEncryptedPreferences.getString(key = "key", null)
        assert(result == "value")
    }

    @Test
    fun defaultValueIsReturnedWhenIncorrectKeyIsProvided() {
        BlueshiftEncryptedPreferences.putString(key = "key", value = "value")
        val result = BlueshiftEncryptedPreferences.getString(key = "wrong_key", "default")
        assert(result == "default")
    }

    @Test
    fun storedValueIsRemovedWhenCallingRemoveMethod() {
        BlueshiftEncryptedPreferences.putString(key = "key", value = "value")

        val result1 = BlueshiftEncryptedPreferences.getString(key = "key", null)
        assert(result1 == "value")

        BlueshiftEncryptedPreferences.remove(key = "key")

        val result2 = BlueshiftEncryptedPreferences.getString(key = "key", null)
        assert(result2 == null)
    }

}
