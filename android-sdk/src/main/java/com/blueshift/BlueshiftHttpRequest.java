package com.blueshift;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

public class BlueshiftHttpRequest {
    private static final String TAG = "BlueshiftHttpRequest";

    public enum Method {
        GET, POST
    }

    private String url;
    private Method method;
    private JSONObject urlParamsJson;
    private JSONObject reqHeaderJson;
    private JSONObject reqBodyJson;

    private BlueshiftHttpRequest() {
        method = Method.GET;
    }

    public Method getMethod() {
        return method;
    }

    public JSONObject getReqHeaderJson() {
        return reqHeaderJson;
    }

    public JSONObject getReqBodyJson() {
        return reqBodyJson;
    }

    public String getUrlWithParams() {
        String finalUrl = url;

        if (url != null && urlParamsJson != null) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            Iterator<String> keys = urlParamsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = urlParamsJson.opt(key);
                if (key != null && !key.equals("") && val != null && !val.equals("")) {
                    if (first) {
                        first = false;
                        builder.append("?");
                    } else {
                        builder.append("&");
                    }

                    String strVal = String.valueOf(val);
                    String encVal = null;

                    try {
                        encVal = URLEncoder.encode(strVal, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        BlueshiftLogger.e(TAG, e);
                    }

                    builder.append(key).append("=").append(encVal != null ? encVal : val);
                }
            }

            finalUrl += builder.toString();
        }

        return finalUrl;
    }

    public static class Builder {
        private String url;
        private Method method;
        private final BlueshiftJSONObject urlParamsJson;
        private final BlueshiftJSONObject reqHeaderJson;
        private final BlueshiftJSONObject reqBodyJson;

        public Builder() {
            method = Method.GET;
            urlParamsJson = new BlueshiftJSONObject();
            reqHeaderJson = new BlueshiftJSONObject();
            reqBodyJson = new BlueshiftJSONObject();
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setMethod(Method method) {
            this.method = method;
            return this;
        }

        public Builder setUrlParamsJson(JSONObject urlParamsJson) {
            this.urlParamsJson.putAll(urlParamsJson);
            return this;
        }

        public Builder setReqHeaderJson(JSONObject reqHeaderJson) {
            this.reqHeaderJson.putAll(reqHeaderJson);
            return this;
        }

        public Builder setReqBodyJson(JSONObject reqBodyJson) {
            this.reqBodyJson.putAll(reqBodyJson);
            return acceptJson();
        }

        public Builder addBasicAuth(String username, String password) {
            if (this.reqHeaderJson != null) {
                String credString = username + ":" + password;
                String base64 = Base64.encodeToString(credString.getBytes(), Base64.DEFAULT);
                String sanitizedBase64 = base64.replace("\n", "");

                try {
                    reqHeaderJson.putOpt("Authorization", "Basic " + sanitizedBase64);
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }

            return this;
        }

        private Builder acceptJson() {
            if (this.reqHeaderJson != null) {
                try {
                    reqHeaderJson.putOpt("Accept", "application/json");
                    reqHeaderJson.putOpt("Content-Type", "application/json");
                } catch (JSONException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }

            return this;
        }

        public BlueshiftHttpRequest build() {
            BlueshiftHttpRequest request = new BlueshiftHttpRequest();
            request.url = url;
            request.method = method;
            request.urlParamsJson = urlParamsJson;
            request.reqHeaderJson = reqHeaderJson;
            request.reqBodyJson = reqBodyJson;
            return request;
        }
    }
}
