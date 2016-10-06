package com.blueshift.batch;

import com.google.gson.Gson;

import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 24/8/16 @ 3:04 PM
 *         https://github.com/rahulrvp
 */
public class Event {
    private long mId;
    private HashMap<String, Object> mEventParams;

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public HashMap<String, Object> getEventParams() {
        return mEventParams;
    }

    public String getEventParamsJson() {
        return new Gson().toJson(mEventParams);
    }

    public void setEventParams(HashMap<String, Object> eventParamsJson) {
        mEventParams = eventParamsJson;
    }
}
