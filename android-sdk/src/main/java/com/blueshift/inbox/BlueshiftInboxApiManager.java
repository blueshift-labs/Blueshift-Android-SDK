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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BlueshiftInboxApiManager {

    @WorkerThread
    @NonNull
    public static List<BlueshiftInboxMessageStatus> getMessageStatuses(@NonNull final Context context) {
        return new ArrayList<>();
    }

    @WorkerThread
    @NonNull
    public static List<BlueshiftInboxMessage> getNewMessages(final Context context, List<String> messageIds) {
        // todo remove the hacky call to inapp api
        // BEGIN
        String apiKey = BlueshiftUtils.getApiKey(context);
        JSONObject requestBody = InAppManager.generateInAppMessageAPIRequestPayload(context);

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


    @WorkerThread
    public static int deleteMessages(List<String> messageIdsToKeep) {
        return 0;
    }

    public static JSONObject transformPayload(String json) {
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
                                JSONObject inappContent = inapp.optJSONObject("content");
                                if (inappContent != null) {
                                    if (inappContent.has("html")) {
                                        inbox.put("title", "HTML");
                                        inbox.put("details", "HTML in-app message");
                                        inbox.put("icon", "");
                                    } else {
                                        inbox.putOpt("title", inappContent.optString("title"));
                                        inbox.putOpt("details", inappContent.optString("message"));
                                        inbox.putOpt("icon", inappContent.optString("icon_image"));
                                    }
                                }
                            }

                            data.putOpt("inbox", inbox);

                            newContent.putOpt("campaign_attr", campaignAttr);
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
