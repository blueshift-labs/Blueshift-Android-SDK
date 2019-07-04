package com.blueshift.inappmessage;

import org.json.JSONObject;

public interface InAppActionCallback {
    void onAction(JSONObject actionArgs);
}
