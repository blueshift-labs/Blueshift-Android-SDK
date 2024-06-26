package com.blueshift.model;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.blueshift.BlueShiftPreference;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftEncryptedPreferences;
import com.blueshift.BlueshiftLogger;
import com.blueshift.util.BlueshiftUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Rahul Raveendran V P
 * Created on 5/3/15 @ 3:05 PM
 * https://github.com/rahulrvp
 */
public class UserInfo {
    private static final String TAG = "UserInfo";
    private static final String PREF_FILE = "user_info_file";
    private static final String PREF_KEY = "user_info_key";
    private static final String PREF_KEY_ENCRYPTED = "user_info";
    private static final Boolean lock = false;

    private String email;
    private String email_hash;
    private String retailer_customer_id;
    private String uuid;
    private String name;
    private String firstname;
    private String lastname;
    private String gender;
    private long joined_at;
    private String facebook_id;
    private String education;
    private boolean unsubscribed;
    private String engagement_score;
    private String purchase_intent;
    private Product[] recommended_products;
    private HashMap<String, Object> details;
    private Date dateOfBirth;

    private static UserInfo instance = null;

    private UserInfo() {
        unsubscribed = false;
    }

    static void killInstance() {
        instance = null;
    }

    public static UserInfo getInstance(Context context) {
        synchronized (lock) {
            if (instance == null) {
                instance = load(context);
                if (instance == null) instance = new UserInfo();
            }

            return instance;
        }
    }

    private static String getSharedPreferencesFilename(@NonNull Context context) {
        return context.getPackageName() + "." + PREF_FILE;
    }

    private static String getSharedPreferencesKey(@NonNull Context context) {
        return context.getPackageName() + "." + PREF_KEY;
    }

    private static UserInfo load(@NonNull Context context) {
        Configuration configuration = BlueshiftUtils.getConfiguration(context);
        boolean isEncryptionEnabled = configuration != null && configuration.shouldSaveUserInfoAsEncrypted();
        return load(context, isEncryptionEnabled);
    }

    static UserInfo load(Context context, boolean encryptionEnabled) {
        return encryptionEnabled ? loadFromEncryptedSharedPreferences(context) : loadFromSharedPreferences(context);
    }

    private static UserInfo loadFromSharedPreferences(@NonNull Context context) {
        UserInfo userInfo = null;

        BlueshiftLogger.d(TAG, "Loading from SharedPreferences.");
        SharedPreferences preferences = context.getSharedPreferences(getSharedPreferencesFilename(context), Context.MODE_PRIVATE);
        String json = preferences.getString(getSharedPreferencesKey(context), null);
        if (json != null) {
            try {
                userInfo = new Gson().fromJson(json, UserInfo.class);
            } catch (JsonSyntaxException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return userInfo;
    }

    private static UserInfo loadFromEncryptedSharedPreferences(@NonNull Context context) {
        UserInfo userInfo = null;

        BlueshiftLogger.d(TAG, "Loading from encrypted preference.");
        String json = BlueshiftEncryptedPreferences.INSTANCE.getString(PREF_KEY_ENCRYPTED, null);
        if (json == null) {
            // The new secure store doesn't have the user info. Let's check in the old preference
            // file and copy over the data if present.
            SharedPreferences pref = context.getSharedPreferences(getSharedPreferencesFilename(context), Context.MODE_PRIVATE);
            String spUserJson = pref.getString(getSharedPreferencesKey(context), null);
            if (spUserJson != null) {
                BlueshiftLogger.d(TAG, "Found user data inside the SharedPreferences. Copying it to the EncryptedSharedPreferences.");
                try {
                    userInfo = new Gson().fromJson(spUserJson, UserInfo.class);
                    // Save it to secure store for loading next time.
                    userInfo.saveToEncryptedSharedPreferences();
                    // Clear the old preference for privacy reasons.
                    BlueshiftLogger.d(TAG, "Clear the SharedPreferences.");
                    pref.edit().clear().apply();
                    // Remove cached email address information (If found)
                    BlueshiftLogger.d(TAG, "Clear the email from SharedPreferences.");
                    BlueShiftPreference.removeCachedEmailAddress(context);
                } catch (Exception e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }
        } else {
            // The new secure store has the user info. Let's load it.
            try {
                userInfo = new Gson().fromJson(json, UserInfo.class);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return userInfo;
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(BlueshiftConstants.KEY_CUSTOMER_ID, getRetailerCustomerId());
        map.put(BlueshiftConstants.KEY_EMAIL, getEmail());
        map.put(BlueshiftConstants.KEY_FIRST_NAME, getFirstname());
        map.put(BlueshiftConstants.KEY_LAST_NAME, getLastname());
        map.put(BlueshiftConstants.KEY_GENDER, getGender());
        map.put(BlueshiftConstants.KEY_FACEBOOK_ID, getFacebookId());
        map.put(BlueshiftConstants.KEY_EDUCATION, getEducation());

        if (getJoinedAt() > 0) {
            map.put(BlueshiftConstants.KEY_JOINED_AT, getJoinedAt());
        }

        if (getDateOfBirth() != null) {
            long seconds = getDateOfBirth().getTime() / 1000;
            map.put(BlueshiftConstants.KEY_DATE_OF_BIRTH, seconds);
        }

        if (isUnsubscribed()) {
            // we don't need to send this key if it set to false
            map.put(BlueshiftConstants.KEY_UNSUBSCRIBED_PUSH, true);
        }

        if (getDetails() != null) {
            for (Map.Entry<String, Object> entry : getDetails().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return map;
    }

    public void save(@NonNull Context context) {
        Configuration configuration = BlueshiftUtils.getConfiguration(context);
        boolean isEncryptionEnabled = configuration != null && configuration.shouldSaveUserInfoAsEncrypted();
        save(context, isEncryptionEnabled);
    }

    void save(Context context, boolean encryptionEnabled) {
        if (encryptionEnabled) {
            saveToEncryptedSharedPreferences();
        } else {
            saveToSharedPreferences(context);
        }
    }

    private void saveToSharedPreferences(Context context) {
        String json = new Gson().toJson(this);
        context.getSharedPreferences(getSharedPreferencesFilename(context), Context.MODE_PRIVATE)
                .edit().putString(getSharedPreferencesKey(context), json).apply();
    }

    private void saveToEncryptedSharedPreferences() {
        String json = new Gson().toJson(this);
        BlueshiftEncryptedPreferences.INSTANCE.putString(PREF_KEY_ENCRYPTED, json);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getRetailerCustomerId() {
        return retailer_customer_id;
    }

    public void setRetailerCustomerId(String retailer_customer_id) {
        this.retailer_customer_id = retailer_customer_id;
    }

    public HashMap<String, Object> getDetails() {
        return details;
    }

    public void setDetails(HashMap<String, Object> details) {
        this.details = details;
    }

    public String getEmail_hash() {
        return email_hash;
    }

    public void setEmailHash(String email_hash) {
        this.email_hash = email_hash;
    }

    public String getUuid() {
        return uuid;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public long getJoinedAt() {
        return joined_at;
    }

    public void setJoinedAt(long joinedAt) {
        this.joined_at = joinedAt;
    }

    public String getFacebookId() {
        return facebook_id;
    }

    public void setFacebookId(String facebookId) {
        this.facebook_id = facebookId;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public boolean isUnsubscribed() {
        return unsubscribed;
    }

    public void setUnsubscribed(boolean unsubscribed) {
        this.unsubscribed = unsubscribed;
    }

    public String getEngagementScore() {
        return engagement_score;
    }

    public String getPurchaseIntent() {
        return purchase_intent;
    }

    public Product[] getRecommendedProducts() {
        return recommended_products;
    }

    public void setDateOfBirth(int birth_day, int birth_month, int birth_year) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dateOfBirth = simpleDateFormat.parse(birth_day + "/" + birth_month + "/" + birth_year);
        } catch (ParseException e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void clear(Context context) {
        synchronized (lock) {
            instance.email = null;
            instance.email_hash = null;
            instance.retailer_customer_id = null;
            instance.uuid = null;
            instance.name = null;
            instance.firstname = null;
            instance.lastname = null;
            instance.gender = null;
            instance.joined_at = 0;
            instance.facebook_id = null;
            instance.education = null;
            instance.unsubscribed = false;
            instance.engagement_score = null;
            instance.purchase_intent = null;
            instance.recommended_products = null;
            instance.details = null;
            instance.dateOfBirth = null;
        }

        save(context);
    }
}
