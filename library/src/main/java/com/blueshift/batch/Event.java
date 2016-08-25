package com.blueshift.batch;

/**
 * Created by rahul on 24/8/16.
 */
public class Event {
    private long mId;
    private String mEventParamsJson;

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public String getEventParamsJson() {
        return mEventParamsJson;
    }

    public void setEventParamsJson(String eventParamsJson) {
        mEventParamsJson = eventParamsJson;
    }
}
