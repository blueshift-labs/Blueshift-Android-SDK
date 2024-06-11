package com.blueshift

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object BlueshiftEncryptedPreferences {
    private const val PREF_NAME = "com.blueshift.encrypted.preferences"

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        sharedPreferences = EncryptedSharedPreferences.create(
            PREF_NAME,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun remove(key: String) {
        if (::sharedPreferences.isInitialized) {
            sharedPreferences.edit().remove(key).apply()
        }
    }

    fun putString(key: String, value: String?) {
        if (::sharedPreferences.isInitialized) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    fun getString(key: String, defaultValue: String?): String? {
        return if (::sharedPreferences.isInitialized) {
            sharedPreferences.getString(key, defaultValue)
        } else {
            defaultValue
        }
    }
}
