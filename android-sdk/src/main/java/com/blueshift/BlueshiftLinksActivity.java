package com.blueshift;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This Activity is expected to receive the deep-links intended to be received
 * by the SDK.
 */
public class BlueshiftLinksActivity extends AppCompatActivity {

    private static final String TAG = "BlueshiftLinksActivity";

    private OnRedirectionListener mListener = new OnRedirectionListener() {
        @Override
        public void onRedirectUrlFetchStart() {

        }

        @Override
        public void onRedirectUrlReceived(Uri uri) {
            redirect(uri);
            finish();
        }

        @Override
        public void onRedirectUrlError(Uri originalUri) {
            redirect(originalUri);
            finish();
        }
    };

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
    }

    private void track(Uri uri) {
        try {
            Blueshift.getInstance(getApplicationContext()).trackUniversalLinks(uri);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void parseAndRedirect(Uri uri) {
        try {
            if (isBlueshiftShortURL(uri)) {
                handleBlueshiftShortURL(uri);
            } else if (isBlueshiftTrackURL(uri)) {
                handleBlueshiftTrackURL(uri);
            } else {
                redirect(uri);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private boolean isBlueshiftShortURL(Uri uri) {
        return uri != null && !TextUtils.isEmpty(uri.getPath()) && uri.getPath().startsWith("/z/");
    }

    private void handleBlueshiftShortURL(final Uri uri) {
        if (uri != null) {
            new HandleBlueshiftShortLinkTask().setCallback(getOnRedirectionListener()).execute(uri);
        }
    }

    private boolean isBlueshiftTrackURL(Uri uri) {
        return uri != null && !TextUtils.isEmpty(uri.getPath()) && uri.getPath().startsWith("/track");
    }

    private void handleBlueshiftTrackURL(Uri uri) {
        if (uri != null) {
            String redirectUrl = uri.getQueryParameter(BlueshiftConstants.KEY_REDIR);
            if (!TextUtils.isEmpty(redirectUrl)) {
                try {
                    Uri redirectUri = Uri.parse(redirectUrl);
                    BlueshiftLogger.d(TAG, "redirect URL found in the given URL. " + redirectUri.toString());
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
    }

    public OnRedirectionListener getOnRedirectionListener() {
        return mListener;
    }

    protected void setOnRedirectionListener(OnRedirectionListener listener) {
        mListener = listener;
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
        } else {
            Intent openAppIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (openAppIntent != null) {
                // Note: This will create a new task and launch the app with the corresponding
                // activity. As per the docs, the dev should add parent activity to all the
                // activities registered in the manifest in order to get the back stack working
                // doc: https://developer.android.com/training/notify-user/navigation#DirectEntry
                TaskStackBuilder.create(this).addNextIntentWithParentStack(openAppIntent).startActivities();
            }
        }

        // close this page once redirection is complete.
        finish();
    }

    private interface OnRedirectionListener {
        void onRedirectUrlFetchStart();

        void onRedirectUrlReceived(Uri uri);

        void onRedirectUrlError(Uri originalUri);
    }

    private static class HandleBlueshiftShortLinkTask extends AsyncTask<Uri, Void, Uri> {

        private OnRedirectionListener mListener;

        public HandleBlueshiftShortLinkTask setCallback(OnRedirectionListener listener) {
            mListener = listener;
            return this;
        }

        @Override
        protected void onPreExecute() {
            if (mListener != null) mListener.onRedirectUrlFetchStart();
        }

        @Override
        protected Uri doInBackground(Uri... uris) {
            Uri uri = uris != null && uris.length > 0 ? uris[0] : null;
            if (uri == null) return null;

            HttpURLConnection httpURLConnection = null;
            Uri redirUri = null;

            try {
                long start = System.currentTimeMillis();

                URL url = new URL(uri.toString());
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setInstanceFollowRedirects(false);
                String location = httpURLConnection.getHeaderField("Location");
                redirUri = Uri.parse(location);

                long diff = System.currentTimeMillis() - start;
                BlueshiftLogger.d(TAG, "Time in ms: " + diff);

                BlueshiftLogger.d(TAG, redirUri.toString());
            } catch (MalformedURLException e) {
                BlueshiftLogger.e(TAG, e);
            } catch (IOException e) {
                BlueshiftLogger.e(TAG, e);
            } finally {
                if (httpURLConnection != null) httpURLConnection.disconnect();
            }

            return redirUri;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (mListener != null) {
                if (uri != null) {
                    mListener.onRedirectUrlReceived(uri);
                } else {
                    mListener.onRedirectUrlError(null);
                }
            }
        }
    }
}
