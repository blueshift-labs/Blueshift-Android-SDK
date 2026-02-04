package com.blueshift;

import android.app.Activity;

import com.blueshift.inappmessage.InAppMessage;

import java.util.Map;

public interface BlueshiftInAppListener {
    void onInAppDelivered(Map<String, Object> attributes);

    void onInAppOpened(Map<String, Object> attributes);

    void onInAppClicked(Map<String, Object> attributes);

    boolean handleInAppRendering(Activity activity, InAppMessage inAppMessage);

}
