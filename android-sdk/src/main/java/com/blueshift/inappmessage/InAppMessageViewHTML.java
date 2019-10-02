package com.blueshift.inappmessage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.blueshift.BlueshiftLogger;
import com.blueshift.model.Configuration;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

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

            webView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            return webView;
        }

        return null;
    }

    private void launchUri(Uri uri) {
        try {
            if (uri != null) {
                Log.d(TAG, "URL: " + uri.toString());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                getContext().startActivity(intent);

                // dismiss dialog.
                onDismiss(getInAppMessage(), uri.toString());
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
            onDismiss(getInAppMessage(), InAppConstants.ACTION_DISMISS);
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
                onDismiss(getInAppMessage(), InAppConstants.ACTION_DISMISS);
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
                onDismiss(getInAppMessage(), InAppConstants.ACTION_DISMISS);
            }

            return true;
        }
    }
}
