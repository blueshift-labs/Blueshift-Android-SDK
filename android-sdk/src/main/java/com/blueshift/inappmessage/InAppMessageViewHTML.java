package com.blueshift.inappmessage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.model.Configuration;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class InAppMessageViewHTML extends InAppMessageView {
    private static final String TAG = InAppMessageViewHTML.class.getSimpleName();

    public InAppMessageViewHTML(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View getView(InAppMessage inAppMessage) {
        String htmlContent = inAppMessage.getContentString(InAppConstants.HTML);
        if (!TextUtils.isEmpty(htmlContent)) {
            WebView webView = new WebView(getContext());

            // taking consent from dev to enable js
            Configuration config = BlueshiftUtils.getConfiguration(getContext());
            if (config != null && config.isJavaScriptForInAppWebViewEnabled()) {
                webView.getSettings().setJavaScriptEnabled(true);
            }

            webView.setWebViewClient(new InAppWebViewClient());
            webView.loadData(CommonUtils.getBase64(htmlContent), "text/html; charset=UTF-8", "base64");

            webView.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

            return webView;
        }

        return null;
    }

    private void launchUri(Uri uri) {
        InAppActionCallback actionCallback = InAppManager.getActionCallback();
        if (actionCallback != null && uri != null) {
            String link = uri.toString();

            JSONObject statsParams = getClickStatsJSONObject(null);
            try {
                if (!TextUtils.isEmpty(link)) {
                    statsParams.putOpt(
                            BlueshiftConstants.KEY_CLICK_URL,
                            NetworkUtils.encodeUrlParam(link));
                }
            } catch (JSONException ignore) {
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(InAppConstants.ANDROID_LINK, link);
            } catch (JSONException ignored) {
            }
            actionCallback.onAction(InAppConstants.ACTION_OPEN, jsonObject);

            onDismiss(getInAppMessage(), statsParams);
        } else {
            openUri(uri);
        }
    }

    private void openUri(Uri uri) {
        if (uri != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                getContext().startActivity(intent);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }

            JSONObject statsParams = getClickStatsJSONObject(null);
            try {
                String link = uri.toString();
                if (!TextUtils.isEmpty(link)) {
                    statsParams.putOpt(
                            BlueshiftConstants.KEY_CLICK_URL,
                            NetworkUtils.encodeUrlParam(link));
                }
            } catch (JSONException ignored) {
            }

            onDismiss(getInAppMessage(), statsParams);
        } else {
            onDismiss(getInAppMessage(), null);
        }
    }

    private class InAppWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Uri uri = request.getUrl();
                    launchUri(uri);
                }
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
                onDismiss(getInAppMessage(), getClickStatsJSONObject(null));
            }

            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                Uri uri = Uri.parse(url);
                launchUri(uri);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
                onDismiss(getInAppMessage(), getClickStatsJSONObject(null));
            }

            return true;
        }
    }
}
