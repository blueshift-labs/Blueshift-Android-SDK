package com.blueshift.inbox;

import android.content.Context;

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
    public static List<BlueshiftInboxMessageStatus> getMessageStatuses(@NonNull final Context context) {
        String deviceId = DeviceUtils.getDeviceId(context);
        if (deviceId == null || deviceId.isEmpty()) {
            BlueshiftLogger.d(TAG, "device_id is not available. try again later.");
        } else {
            String apiKey = BlueshiftUtils.getApiKey(context);
            if (apiKey != null) {
                JSONObject body = InAppManager.generateInAppMessageAPIRequestPayload(context);
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
                        List<BlueshiftInboxMessageStatus> statuses = new ArrayList<>();
                        if (content != null && content.length() > 0) {
                            for (int i = 0; i < content.length(); i++) {
                                statuses.add(new BlueshiftInboxMessageStatus(content.getJSONObject(i)));
                            }
                        }
                        return statuses;
                    } catch (JSONException ignore) {
                    }
                } else {
                    BlueshiftLogger.e(TAG, response.getBody());
                }
            }
        }

        // NULL indicates internet unavailability or api error
        return null;
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

                JSONObject body = InAppManager.generateInAppMessageAPIRequestPayload(context);
                try {
                    if (body != null) {
                        body.put("message_uuids", jsonArray);
                    }
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
                        List<BlueshiftInboxMessage> messages = new ArrayList<>();
                        if (content != null && content.length() > 0) {
                            for (int i = 0; i < content.length(); i++) {
                                messages.add(new BlueshiftInboxMessage(content.getJSONObject(i)));
                            }
                        }
                        return messages;
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

                JSONObject body = InAppManager.generateInAppMessageAPIRequestPayload(context);
                try {
                    if (body != null) {
                        body.put("action", "delete");
                        body.put("message_uuids", jsonArray);
                    }
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

    @WorkerThread
    public static List<BlueshiftInboxMessage> getNewMessagesLegacy(Context context) {
        String apiKey = BlueshiftUtils.getApiKey(context);
        JSONObject requestBody = InAppManager.generateInAppMessageAPIRequestPayload(context);

        BlueshiftHttpRequest.Builder builder = new BlueshiftHttpRequest.Builder()
                .setUrl(BlueshiftConstants.IN_APP_API_URL(context))
                .setMethod(BlueshiftHttpRequest.Method.POST)
                .addBasicAuth(apiKey, "")
                .setReqBodyJson(requestBody);

        BlueshiftHttpResponse response = BlueshiftHttpManager.getInstance().send(builder.build());
        String responseBody = response.getBody();

        JSONObject finalResponse = transformPayload(responseBody);
        JSONArray content = finalResponse.optJSONArray("content");
        if (content != null) {
            return BlueshiftInboxMessage.fromJsonArray(content);
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
                    JSONObject content = jsonArray.optJSONObject(i);
                    if (content != null) {
                        JSONObject data = content.optJSONObject("data");
                        if (data != null) {
                            newContent.putOpt("created_at", data.optString("timestamp"));
                            newContent.putOpt("message_uuid", data.optString("bsft_message_uuid"));
                            newContent.putOpt("user_uuid", data.optString("bsft_user_uuid"));
                            newContent.putOpt("account_uuid", data.optString("bsft_user_uuid"));
                            newContent.putOpt("status", "unread");

                            JSONObject inapp = data.optJSONObject("inapp");
                            if (inapp != null) {
                                inapp.putOpt("scope", BlueshiftInboxMessage.Scope.INAPP_ONLY.toString());
                                data.putOpt("inapp", inapp);
                            }

                            newContent.putOpt("data", data);

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
