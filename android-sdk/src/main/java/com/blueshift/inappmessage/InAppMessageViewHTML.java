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
import com.blueshift.util.InAppUtils;

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

            int width = getWebViewDimension(inAppMessage, InAppConstants.WIDTH);
            int height = getWebViewDimension(inAppMessage, InAppConstants.HEIGHT);

            webView.setLayoutParams(new ViewGroup.LayoutParams(width, height));

            return webView;
        }

        return null;
    }

    /**
     * Decides what should be the dimension of the {@link WebView} based on the availability
     * of height and width.
     *
     * @param inAppMessage  valid {@link InAppMessage} object
     * @param dimensionName mentions width or height needs to be retrieved
     * @return MATCH_PARENT if a dimension is available, else use WRAP_CONTENT
     */
    private int getWebViewDimension(InAppMessage inAppMessage, String dimensionName) {
        int width = InAppUtils.getTemplateStyleInt(getContext(), inAppMessage, dimensionName, -1);
        return width > 0 ? MATCH_PARENT : WRAP_CONTENT;
    }

    private void launchUri(Uri uri) {
        if (InAppUtils.isDismissURL(uri)) {
            handleDismiss(getInAppMessage(), null);
        } else {
            InAppActionCallback actionCallback = InAppManager.getActionCallback();
            if (actionCallback != null && uri != null) {
                String link = uri.toString();

                JSONObject statsParams = getClickStatsJSONObject(null);
                try {
                    if (!TextUtils.isEmpty(link)) {
                        statsParams.putOpt(BlueshiftConstants.KEY_CLICK_URL, link);
                    }
                } catch (JSONException ignore) {
                }

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(InAppConstants.ANDROID_LINK, link);
                } catch (JSONException ignored) {
                }
                actionCallback.onAction(InAppConstants.ACTION_OPEN, jsonObject);

                handleClick(getInAppMessage(), statsParams);
            } else {
                openUri(uri);
            }
        }
    }

    private void openUri(Uri uri) {
        if (InAppUtils.isDismissURL(uri)) {
            handleDismiss(getInAppMessage(), null);
        } else if (uri != null) {
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
                    statsParams.putOpt(BlueshiftConstants.KEY_CLICK_URL, link);
                }
            } catch (JSONException ignored) {
            }

            handleClick(getInAppMessage(), statsParams);
        } else {
            handleClick(getInAppMessage(), null);
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
                handleClick(getInAppMessage(), getClickStatsJSONObject(null));
            }

            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                Uri uri = Uri.parse(url);
                launchUri(uri);
            } catch (Exception e) {
                handleClick(getInAppMessage(), getClickStatsJSONObject(null));
            }

            return true;
        }
    }
}
