package com.blueshift;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class BlueshiftJSONObject extends JSONObject {

    public void putAll(JSONObject jsonObject) throws JSONException {
        if (jsonObject != null) {
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                put(key, jsonObject.get(key));
            }
        }
    }
}
