package com.blueshift.model

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.blueshift.BlueshiftEncryptedPreferences
import org.junit.Before
import org.junit.Test

class UserInfoTest {
    private lateinit var context: Context
    private lateinit var legacyPreference: SharedPreferences

    private fun oldPreferenceFile(context: Context): String {
        return context.packageName + ".user_info_file"
    }

    private fun oldPreferenceKey(context: Context): String {
        return context.packageName + ".user_info_key"
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Reset old preferences
        legacyPreference = context.getSharedPreferences(
            oldPreferenceFile(context), Context.MODE_PRIVATE
        )
        legacyPreference.edit().remove(oldPreferenceKey(context)).commit()

        // Reset new preferences
        BlueshiftEncryptedPreferences.init(context)
        BlueshiftEncryptedPreferences.remove(PREF_KEY)
    }

    @Test
    fun load_newInstall_returnEmptyUserInfoObject() {
        // For a fresh installation, the shared preferences will not contain any data.
        // So, the user info class should provide an instance without any value for its members.
        val user = UserInfo.getInstance(context)
        assert(user.name == null)
    }

    @Test
    fun load_updatedFromOldSDK_returnSameUserInfoObject() {
        // Mock the presence of a user object in the old preference.
        legacyPreference.edit().putString(oldPreferenceKey(context), USER_JSON).apply()

        // The loaded user info object should provide the same value for its members as we saved in the old preference.
        val name = UserInfo.getInstance(context).name
        assert(name == "name")

        // Kill the existing instance for the next test.
        UserInfo.killInstance()
    }

    @Test
    fun load_updatedFromOldSDK_copiesTheContentOfOldPrefToNewPref() {
        // Mock the presence of a user object in the old preference.
        legacyPreference.edit().putString(oldPreferenceKey(context), USER_JSON).apply()

        UserInfo.getInstance(context)

        // Double check the value stored in the secure store is same as the value in the old preference.
        val json = BlueshiftEncryptedPreferences.getString(PREF_KEY, null)
        assert(USER_JSON == json)

        // Kill the existing instance for the next test.
        UserInfo.killInstance()
    }

    @Test
    fun load_updatedFromOldSDK_deletesTheDataInOldPreference() {
        // Mock the presence of a user object in the old preference.
        legacyPreference.edit().putString(oldPreferenceKey(context), USER_JSON).apply()

        UserInfo.getInstance(context)

        // Make sure the value stored in the old preferences is removed after copying it to the new preferences.
        val legacyJson = legacyPreference.getString(oldPreferenceKey(context), null)
        assert(legacyJson == null)

        // Kill the existing instance for the next test.
        UserInfo.killInstance()
    }

    companion object {
        const val PREF_KEY = "user_info"
        const val USER_JSON = "{\"joined_at\":0,\"name\":\"name\",\"unsubscribed\":false}"
    }
}
