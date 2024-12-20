package com.blueshift;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class BlueshiftJSONObject extends JSONObject {

    public synchronized void putAll(JSONObject jsonObject) {
        if (jsonObject != null) {
            try {
                JSONArray names = jsonObject.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String key = names.getString(i);
                        Object value = jsonObject.get(key);
                        put(key, value);
                    }
                }
            } catch (Exception exception) {
                BlueshiftLogger.e("BlueshiftJSONObject", exception);
            }
        }
    }

    public synchronized HashMap<String, Object> toHasMap() {
        HashMap<String, Object> map = new HashMap<>();

        Iterator<String> keys = keys();
        while (keys.hasNext()) {
            try {
                String key = keys.next();
                Object val = get(key);
                map.put(key, val);
            } catch (JSONException exception) {
                BlueshiftLogger.e("BlueshiftJSONObject", exception);
            }
        }

        return map;
    }
}
