package com.blueshift;

import java.util.Map;

public interface BlueshiftInAppListener {
    void onInAppDelivered(Map<String, Object> attributes);

    void onInAppOpened(Map<String, Object> attributes);

    void onInAppClicked(Map<String, Object> attributes);
}
