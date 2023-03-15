package com.blueshift.inbox;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftHttpManager;
import com.blueshift.BlueshiftHttpRequest;
import com.blueshift.BlueshiftHttpResponse;
import com.blueshift.BlueshiftLogger;
import com.blueshift.inappmessage.InAppManager;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.DeviceUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BlueshiftInboxApiManager {
    private static final String TAG = "InboxApiManager";

    @WorkerThread
    @NonNull
    public static List<BlueshiftInboxMessageStatus> getMessageStatuses(@NonNull final Context context) {
        String deviceId = DeviceUtils.getDeviceId(context);
        if (deviceId == null || deviceId.isEmpty()) {
            BlueshiftLogger.d(TAG, "device_id is not available. try again later.");
        } else {
            String apiKey = BlueshiftUtils.getApiKey(context);
            if (apiKey != null) {
                JSONObject body = new JSONObject();
                try {
                    body.put("api_key", apiKey);
                    body.put("device_id", deviceId);
                } catch (JSONException ignore) {
                }
                BlueshiftHttpRequest request = new BlueshiftHttpRequest.Builder()
                        .setUrl(BlueshiftConstants.INBOX_STATUS(context))
                        .addBasicAuth(apiKey, "")
                        .setReqBodyJson(body)
                        .setMethod(BlueshiftHttpRequest.Method.POST)
                        .build();
                BlueshiftHttpResponse response = BlueshiftHttpManager.getInstance().send(request);
                if (response.getCode() == 200) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.getBody());
                        JSONArray content = jsonObject.optJSONArray("content");
                        if (content != null && content.length() > 0) {
                            List<BlueshiftInboxMessageStatus> statuses = new ArrayList<>();
                            for (int i = 0; i < content.length(); i++) {
                                statuses.add(new BlueshiftInboxMessageStatus(content.getJSONObject(i)));
                            }
                            return statuses;
                        }
                    } catch (JSONException ignore) {
                    }
                } else {
                    BlueshiftLogger.e(TAG, response.getBody());
                }
            }
        }

        return new ArrayList<>();
    }

    @WorkerThread
    @NonNull
    public static List<BlueshiftInboxMessage> getNewMessages(final Context context, List<String> messageIds) {
        String deviceId = DeviceUtils.getDeviceId(context);
        if (deviceId == null || deviceId.isEmpty()) {
            BlueshiftLogger.d(TAG, "device_id is not available. try again later.");
        } else {
            String apiKey = BlueshiftUtils.getApiKey(context);
            if (apiKey != null) {
                // prepare ids
                JSONArray jsonArray = new JSONArray();
                for (String id : messageIds) {
                    jsonArray.put(id);
                }

                JSONObject body = new JSONObject();
                try {
                    body.put("api_key", apiKey);
                    body.put("device_id", deviceId);
                    body.put("message_uuids", jsonArray);
                } catch (JSONException ignore) {
                }
                BlueshiftHttpRequest request = new BlueshiftHttpRequest.Builder()
                        .setUrl(BlueshiftConstants.INBOX_MESSAGES(context))
                        .addBasicAuth(apiKey, "")
                        .setReqBodyJson(body)
                        .setMethod(BlueshiftHttpRequest.Method.POST)
                        .build();
                BlueshiftHttpResponse response = BlueshiftHttpManager.getInstance().send(request);
                if (response.getCode() == 200) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.getBody());
                        JSONArray content = jsonObject.optJSONArray("content");
                        if (content != null && content.length() > 0) {
                            List<BlueshiftInboxMessage> messages = new ArrayList<>();
                            for (int i = 0; i < content.length(); i++) {
                                messages.add(new BlueshiftInboxMessage(content.getJSONObject(i)));
                            }
                            return messages;
                        }
                    } catch (JSONException ignore) {
                    }
                } else {
                    BlueshiftLogger.e(TAG, response.getBody());
                }
            }
        }

        return new ArrayList<>();
    }

    @WorkerThread
    public static boolean deleteMessages(Context context, List<String> messageIds) {
        String deviceId = DeviceUtils.getDeviceId(context);
        if (deviceId == null || deviceId.isEmpty()) {
            BlueshiftLogger.d(TAG, "device_id is not available. try again later.");
        } else {
            String apiKey = BlueshiftUtils.getApiKey(context);
            if (apiKey != null) {
                // prepare ids
                JSONArray jsonArray = new JSONArray();
                for (String id : messageIds) {
                    jsonArray.put(id);
                }

                JSONObject body = new JSONObject();
                try {
                    body.put("api_key", apiKey);
                    body.put("device_id", deviceId);
                    body.put("action", "delete");
                    body.put("message_uuids", jsonArray);
                } catch (JSONException ignore) {
                }
                BlueshiftHttpRequest request = new BlueshiftHttpRequest.Builder()
                        .setUrl(BlueshiftConstants.INBOX_UPDATE(context))
                        .addBasicAuth(apiKey, "")
                        .setReqBodyJson(body)
                        .setMethod(BlueshiftHttpRequest.Method.POST)
                        .build();
                BlueshiftHttpResponse response = BlueshiftHttpManager.getInstance().send(request);
                if (response.getCode() == 200) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.getBody());
                        if ("success".equals(jsonObject.optString("status"))) {
                            return true;
                        }
                    } catch (JSONException ignore) {
                    }
                } else {
                    BlueshiftLogger.e(TAG, response.getBody());
                }
            }
        }

        return false;
    }

    private static List<BlueshiftInboxMessage> mockAPICall(Context context) {
        String apiKey = BlueshiftUtils.getApiKey(context);
        JSONObject requestBody = InAppManager.generateInAppMessageAPIRequestPayload(context);
        try {
            if (requestBody != null) {
                requestBody.putOpt(BlueshiftConstants.KEY_LAST_TIMESTAMP, 0);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        BlueshiftHttpRequest.Builder builder = new BlueshiftHttpRequest.Builder()
                .setUrl(BlueshiftConstants.IN_APP_API_URL(context))
                .setMethod(BlueshiftHttpRequest.Method.POST)
                .addBasicAuth(apiKey, "")
                .setReqBodyJson(requestBody);

        BlueshiftHttpResponse response = BlueshiftHttpManager.getInstance().send(builder.build());
        String responseBody = response.getBody();
        // END

        JSONObject finalResponse = transformPayload(responseBody);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            JSONArray content = finalResponse.optJSONArray("content");
            if (content != null) {
                return BlueshiftInboxMessage.fromJsonArray(content);
            }
        }

        return new ArrayList<>();
    }

    private static JSONObject transformPayload(String json) {
        JSONObject transformedResponse = new JSONObject();

        try {
            transformedResponse.putOpt("status", "success");

            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.optJSONArray("content");
            if (jsonArray != null) {
                JSONArray newContents = new JSONArray();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject newContent = new JSONObject();
                    JSONObject campaignAttr = new JSONObject();

                    JSONObject content = jsonArray.optJSONObject(i);
                    if (content != null) {
                        JSONObject data = content.optJSONObject("data");
                        if (data != null) {
                            campaignAttr.putOpt("uid", data.optString("bsft_user_uuid"));
                            campaignAttr.putOpt("mid", data.optString("bsft_message_uuid"));
                            campaignAttr.putOpt("eid", data.optString("bsft_experiment_uuid"));
                            campaignAttr.putOpt("txnid", data.optString("bsft_transaction_uuid"));

                            newContent.putOpt("created_at", data.optString("timestamp"));
                            newContent.putOpt("message_uuid", data.optString("bsft_message_uuid"));
                            newContent.putOpt("user_uuid", data.optString("bsft_user_uuid"));
                            newContent.putOpt("account_uuid", data.optString("bsft_user_uuid"));
                            newContent.putOpt("status", "unread");

                            JSONObject inbox = new JSONObject();
                            JSONObject inapp = data.optJSONObject("inapp");
                            if (inapp != null) {
                                inapp.putOpt("scope", BlueshiftInboxMessage.Scope.INBOX_AND_INAPP.toString());

                                JSONObject inappContent = inapp.optJSONObject("content");
                                if (inappContent != null) {
                                    if (inappContent.has("html")) {
                                        inbox.put("title", "HTML");
                                        inbox.put("details", "HTML in-app message");
                                        inbox.put("icon", "https://picsum.photos/id/" + (60 + i) + "/100/100");
                                    } else {
                                        String title = inappContent.optString("title");
                                        if (title.isEmpty()) {
                                            title = "SLIDE IN";
                                        }
                                        inbox.putOpt("title", title);

                                        inbox.putOpt("details", inappContent.optString("message"));

                                        String icon = inappContent.optString("icon_image");
                                        if (icon.isEmpty()) {
                                            icon = "https://picsum.photos/id/" + (60 + i) + "/100/100";
                                        }
                                        inbox.putOpt("icon", icon);
                                    }
                                }
                            }

                            data.putOpt("inbox", inbox);

                            newContent.putOpt("campaign_attr", campaignAttr);
                            newContent.putOpt("data", data);
                            newContent.putOpt("scope", "inapp+inbox");

                            newContents.put(newContent);
                        }
                    }
                }

                transformedResponse.putOpt("content", newContents);
            }

            BlueshiftLogger.d("", transformedResponse.toString());
        } catch (JSONException e) {
            BlueshiftLogger.e("", e);
        }

        return transformedResponse;
    }
}
