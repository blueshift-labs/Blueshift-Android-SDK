package com.blueshift;

import android.content.Context;

import com.blueshift.model.UserInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class BlueshiftAttrUser extends JSONObject {
    private static final String TAG = "UserAttributes";
    private static final BlueshiftAttrUser instance = new BlueshiftAttrUser();

    public static BlueshiftAttrUser getInstance() {
        synchronized (instance) {
            return instance;
        }
    }

    public BlueshiftAttrUser updateUserAttributesFromUserInfo(Context context) {
        synchronized (instance) {
            UserInfo userInfo = UserInfo.getInstance(context);
            if (userInfo != null) {
                try {
                    instance.putOpt(BlueshiftConstants.KEY_CUSTOMER_ID, userInfo.getRetailerCustomerId());
                    instance.putOpt(BlueshiftConstants.KEY_EMAIL, userInfo.getEmail());
                    instance.putOpt(BlueshiftConstants.KEY_FIRST_NAME, userInfo.getFirstname());
                    instance.putOpt(BlueshiftConstants.KEY_LAST_NAME, userInfo.getLastname());
                    instance.putOpt(BlueshiftConstants.KEY_GENDER, userInfo.getGender());
                    instance.putOpt(BlueshiftConstants.KEY_FACEBOOK_ID, userInfo.getFacebookId());
                    instance.putOpt(BlueshiftConstants.KEY_EDUCATION, userInfo.getEducation());

                    if (userInfo.getJoinedAt() > 0) {
                        instance.putOpt(BlueshiftConstants.KEY_JOINED_AT, userInfo.getJoinedAt());
                    }

                    if (userInfo.getDateOfBirth() != null) {
                        long seconds = userInfo.getDateOfBirth().getTime() / 1000;
                        instance.putOpt(BlueshiftConstants.KEY_DATE_OF_BIRTH, seconds);
                    }

                    if (userInfo.isUnsubscribed()) {
                        // we don't need to send this key if it set to false
                        instance.putOpt(BlueshiftConstants.KEY_UNSUBSCRIBED_PUSH, true);
                    }

                    if (userInfo.getDetails() != null) {
                        for (Map.Entry<String, Object> entry : userInfo.getDetails().entrySet()) {
                            instance.putOpt(entry.getKey(), entry.getValue());
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
