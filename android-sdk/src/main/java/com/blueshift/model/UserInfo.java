package com.blueshift.model;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author Rahul Raveendran V P
 *         Created on 5/3/15 @ 3:05 PM
 *         https://github.com/rahulrvp
 */
public class UserInfo {
    private static final String PREF_FILE = "user_info_file";
    private static final String PREF_KEY = "user_info_key";

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

    public static UserInfo getInstance(Context context) {
        if (instance == null) {
            instance = load(context);
            if (instance == null) {
                instance = new UserInfo();
            }
        }

        return instance;
    }

    private static String getPrefFile(Context context) {
        return context.getPackageName() + "." + PREF_FILE;
    }

    private static String getPrefKey(Context context) {
        return context.getPackageName() + "." + PREF_KEY;
    }

    private static UserInfo load(Context context) {
        UserInfo userInfo = null;
        String json = context.getSharedPreferences(getPrefFile(context), Context.MODE_PRIVATE)
                .getString(getPrefKey(context), null);
        if (json != null) {
            try {
                userInfo = new Gson().fromJson(json, UserInfo.class);
            } catch (JsonSyntaxException e) { e.printStackTrace(); }
        }

        return userInfo;
    }

    public void save(Context context) {
        context.getSharedPreferences(getPrefFile(context), Context.MODE_PRIVATE)
                .edit()
                .putString(getPrefKey(context), new Gson().toJson(this))
                .apply();
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
            e.printStackTrace();
        }
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }
}
