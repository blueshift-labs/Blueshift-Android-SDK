package com.blueshift.batch;

import com.google.gson.Gson;

import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 24/8/16 @ 3:04 PM
 *         https://github.com/rahulrvp
 *
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
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
