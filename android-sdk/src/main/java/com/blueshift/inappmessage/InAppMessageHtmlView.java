package com.blueshift.inappmessage;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InAppMessageHtmlView extends InAppMessageView {
    private static final String CONTENT_HTML = "html";

    public InAppMessageHtmlView(Context context, InAppMessage inAppMessage) {
        super(context, inAppMessage);
    }

    @Override
    public View getView(InAppMessage inAppMessage) {
        String htmlContent = inAppMessage.getContentString(CONTENT_HTML);
        if (!TextUtils.isEmpty(htmlContent)) {
            WebView webView = new WebView(getContext());
            webView.setWebViewClient(new WebViewClient());
            webView.loadData(htmlContent, "text/html; charset=UTF-8", null);

            // Note: For WebView, the parent's size depends on the size of WebView, so we are
            // using the same dimensions of the template for this view.
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                    inAppMessage.getTemplateWidth(getContext()),
                    inAppMessage.getTemplateHeight(getContext()));

            webView.setLayoutParams(lp);

            return webView;
        }

        return null;
    }
}
