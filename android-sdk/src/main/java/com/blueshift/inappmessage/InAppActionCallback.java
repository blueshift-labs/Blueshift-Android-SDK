package com.blueshift.inappmessage;

import org.json.JSONObject;

public interface InAppActionCallback {
    void onAction(String actionName, JSONObject actionArgs);
}
