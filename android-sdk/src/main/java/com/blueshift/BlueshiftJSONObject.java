package com.blueshift;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BlueshiftJSONObject extends JSONObject {

    public void putAll(JSONObject jsonObject) {
        if (jsonObject != null) {
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                try {
                    String key = keys.next();
                    Object val = jsonObject.get(key);
                    putOpt(key, val);
                } catch (JSONException ignored) {
                }
            }
        }
    }

    public void putAll(Map<String, Object> map){
        if (map != null) {
            for (String key : map.keySet()) {
                try {
                    putOpt(key, map.get(key));
                } catch (JSONException ignored) {
                }
            }
        }
    }

    public HashMap<String, Object> toHasMap() {
        HashMap<String, Object> map = new HashMap<>();

        Iterator<String> keys = keys();
        while (keys.hasNext()) {
            try {
                String key = keys.next();
                Object val = get(key);
                map.put(key, val);
            } catch (JSONException ignored) {
            }
        }

        return map;
    }
}
