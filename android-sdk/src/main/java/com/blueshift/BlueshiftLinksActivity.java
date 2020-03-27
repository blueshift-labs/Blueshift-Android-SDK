package com.blueshift;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

/**
 * This Activity is expected to receive the deep-links intended to be received
 * by the SDK.
 */
public class BlueshiftLinksActivity extends AppCompatActivity {

    private static final String TAG = "BlueshiftLinksActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            Uri deepLink = getIntent().getData();
            if (deepLink == null) {
                BlueshiftLogger.e(TAG, "No URL found inside the Intent.");
            } else {
                track(deepLink);
                parseAndRedirect(deepLink);
            }
        }

        finish();
    }

    private void track(Uri uri) {
        try {
            Blueshift.getInstance(this).trackUniversalLinks(uri);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void parseAndRedirect(Uri uri) {
        try {
            if (uri != null) {
                String redirectUrl = uri.getQueryParameter(BlueshiftConstants.KEY_REDIR);
                if (!TextUtils.isEmpty(redirectUrl)) {
                    try {
                        Uri redirectUri = Uri.parse(redirectUrl);
                        BlueshiftLogger.d(TAG, "Redirect URL found in the given URL. " + redirectUri.toString());
                        redirect(redirectUri);
                    } catch (Exception e) {
                        BlueshiftLogger.d(TAG, "Invalid redirect URL found in the given URL. " + uri.toString());
                        redirect(null);
                    }
                } else {
                    BlueshiftLogger.d(TAG, "No redirect URL found in the given URL. " + uri.toString());
                    redirect(null);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void openLink(Uri link, Bundle extras) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(link);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            if (extras != null) {
                intent.putExtras(extras);
            }

            startActivity(intent);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    /**
     * The host app can override this method and implement their own logc to handle the uri
     * if they wish to do so.
     *
     * @param uri nullable uri object to launch
     */
    protected void redirect(@Nullable Uri uri) {
        if (uri != null) {
            openLink(uri, getIntent().getExtras());
        }
    }
}
