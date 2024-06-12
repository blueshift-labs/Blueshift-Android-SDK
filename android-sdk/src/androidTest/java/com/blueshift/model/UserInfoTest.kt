package com.blueshift.model

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.blueshift.BlueshiftEncryptedPreferences
import org.junit.Before
import org.junit.Test

class UserInfoTest {
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    private fun sharedPreferencesFilename(context: Context): String {
        return context.packageName + ".user_info_file"
    }

    private fun sharedPreferencesKey(context: Context): String {
        return context.packageName + ".user_info_key"
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Reset old preferences
        sharedPreferences = context.getSharedPreferences(
            sharedPreferencesFilename(context), Context.MODE_PRIVATE
        )
        sharedPreferences.edit().remove(sharedPreferencesKey(context)).commit()

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
        val emailPreferencesKey = "john.doe@examplepetstore.com"
        val emailPreferencesFileName = "${context.packageName}.BsftEmailPrefFile"
        val emailPreferences = context.getSharedPreferences(emailPreferencesFileName, Context.MODE_PRIVATE)
        emailPreferences.edit().putBoolean(emailPreferencesKey, true).apply()

        // Mock the presence of a user object in the old preference.
        sharedPreferences.edit().putString(sharedPreferencesKey(context), USER_JSON).apply()

        // When encryption is not enabled, the user info class should provide the same value
        // for its members as we saved in the old preference.
        val name = UserInfo.load(context, false).name
        assert(name == JOHN)

        // When encryption is enabled, the user info class should provide the same value
        // for its members as we saved in the old preference.
        val encryptedName = UserInfo.load(context, true).name
        assert(encryptedName == JOHN)

        // When encryption is enabled, the data stored in email preferences (if any) should be deleted.
        val status = emailPreferences.getBoolean(emailPreferencesKey, false)
        assert(!status)

        // Kill the existing instance for the next test.
        UserInfo.killInstance()
    }

    @Test
    fun load_updatedFromOldSDK_copiesTheContentOfOldPrefToNewPref() {
        // Mock the presence of a user object in the old preference.
        sharedPreferences.edit().putString(sharedPreferencesKey(context), USER_JSON).apply()

        // case1 : When encryption is not enabled.
        val userInfo = UserInfo.load(context, false)
        assert(userInfo.name == JOHN)

        // case2 : When encryption is enabled.
        UserInfo.load(context, true)
        val json = BlueshiftEncryptedPreferences.getString(PREF_KEY, null)
        assert(USER_JSON == json)

        // Kill the existing instance for the next test.
        UserInfo.killInstance()
    }

    @Test
    fun load_updatedFromOldSDK_deletesTheDataInOldPreference() {
        // Mock the presence of a user object in the old preference.
        sharedPreferences.edit().putString(sharedPreferencesKey(context), USER_JSON).apply()

        // case1 : When encryption is not enabled.
        val userInfo = UserInfo.load(context, false)
        assert(userInfo.name == JOHN)

        // case2 : When encryption is enabled.
        UserInfo.load(context, true)
        // Make sure the value stored in the old preferences is removed after copying it to the new preferences.
        val spUserJson = sharedPreferences.getString(sharedPreferencesKey(context), null)
        assert(spUserJson == null)

        // Kill the existing instance for the next test.
        UserInfo.killInstance()
    }

    companion object {
        const val JOHN = "John"
        const val PREF_KEY = "user_info"
        const val USER_JSON = "{\"joined_at\":0,\"name\":\"$JOHN\",\"unsubscribed\":false}"
    }
}
