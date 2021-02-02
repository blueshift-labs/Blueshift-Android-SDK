package com.blueshift;

import java.util.Map;

public interface BlueshiftInAppListener {
    void onInAppDelivered(Map<String, String> attributes);

    void onInAppOpened(Map<String, String> attributes);

    void onInAppClicked(Map<String, String> attributes);
}
