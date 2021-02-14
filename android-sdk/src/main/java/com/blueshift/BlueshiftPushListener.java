package com.blueshift;

import java.util.Map;

public interface BlueshiftPushListener {
    void onPushDelivered(Map<String, Object> attributes);

    void onPushClicked(Map<String, Object> attributes);
}
