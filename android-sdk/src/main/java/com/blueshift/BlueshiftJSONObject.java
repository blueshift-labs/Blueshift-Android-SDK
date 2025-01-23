package com.blueshift;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

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

        try {
            JSONArray names = names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.getString(i);
                    Object value = get(key);
                    map.put(key, value);
                }
            }
        } catch (Exception exception) {
            BlueshiftLogger.e("BlueshiftJSONObject", exception);
        }

        return map;
    }
}
