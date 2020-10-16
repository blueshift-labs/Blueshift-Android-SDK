package com.blueshift;

import android.content.Context;

import com.blueshift.model.UserInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class BlueshiftAttributesUser extends JSONObject {
    private static final String TAG = "UserAttributes";
    private static final BlueshiftAttributesUser instance = new BlueshiftAttributesUser();

    public static BlueshiftAttributesUser getInstance() {
        synchronized (instance) {
            return instance;
        }
    }

    public BlueshiftAttributesUser sync(Context context) {
        synchronized (instance) {
            UserInfo userInfo = UserInfo.getInstance(context);
            if (userInfo != null) {
                try {
                    instance.put(BlueshiftConstants.KEY_CUSTOMER_ID, userInfo.getRetailerCustomerId());
                    instance.put(BlueshiftConstants.KEY_EMAIL, userInfo.getEmail());
                    instance.put(BlueshiftConstants.KEY_FIRST_NAME, userInfo.getFirstname());
                    instance.put(BlueshiftConstants.KEY_LAST_NAME, userInfo.getLastname());
                    instance.put(BlueshiftConstants.KEY_GENDER, userInfo.getGender());
                    instance.put(BlueshiftConstants.KEY_FACEBOOK_ID, userInfo.getFacebookId());
                    instance.put(BlueshiftConstants.KEY_EDUCATION, userInfo.getEducation());

                    if (userInfo.getJoinedAt() > 0) {
                        instance.put(BlueshiftConstants.KEY_JOINED_AT, userInfo.getJoinedAt());
                    }

                    if (userInfo.getDateOfBirth() != null) {
                        long seconds = userInfo.getDateOfBirth().getTime() / 1000;
                        instance.put(BlueshiftConstants.KEY_DATE_OF_BIRTH, seconds);
                    }

                    if (userInfo.isUnsubscribed()) {
                        // we don't need to send this key if it set to false
                        instance.put(BlueshiftConstants.KEY_UNSUBSCRIBED_PUSH, true);
                    }

                    if (userInfo.getDetails() != null) {
                        for (Map.Entry<String, Object> entry : userInfo.getDetails().entrySet()) {
                            instance.put(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }

            return instance;
        }
    }

    public void log(){
        synchronized (instance) {
            BlueshiftLogger.v(TAG, instance.toString());
        }
    }
}
