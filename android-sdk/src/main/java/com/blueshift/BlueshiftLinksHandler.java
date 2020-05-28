package com.blueshift;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;


@SuppressWarnings("WeakerAccess")
public class BlueshiftLinksHandler {

    private static final String TAG = "BlueshiftLinksHandler";
    private BlueshiftLinksListener listener;
    private Context context;
    private Bundle extras;

    public BlueshiftLinksHandler(Context context) {
        this.context = context;
    }

    public boolean handleBlueshiftUniversalLinks(Intent intent, BlueshiftLinksListener listener) {
        if (intent != null) {
            return handleBlueshiftUniversalLinks(intent.getData(), intent.getExtras(), listener);
        }

        return false;
    }

    public boolean handleBlueshiftUniversalLinks(Uri data, Bundle extras, BlueshiftLinksListener listener) {
        this.extras = extras;
        this.listener = listener;

        if (data != null) {
            if (isShortUrlWithZ(data)) {
                handleShortURL(data);
            } else if (isTrackUrl(data)) {
                invokeTrackAPICall(data);
                handleTrackURL(data);
            } else {
                invokeOnLinkProcessingStart();
                BlueshiftLogger.d(TAG, "Non-Blueshift URL detected: " + data);
                if (hasBlueshiftParamsInURL(data)) {
                    BlueshiftLogger.d(TAG, "Blueshift ids found in non-Blueshift URL. Tracking..");
                    invokeTrackAPICall(data);
                }
                invokeOnLinkProcessingComplete(data);
            }
        }

        return isBlueshiftLink(data);
    }

    private boolean isBlueshiftLink(Uri uri) {
        return isQueryParameterPresent(uri, "uid") && isQueryParameterPresent(uri, "mid");
    }

    private boolean hasBlueshiftParamsInURL(Uri uri) {
        return isQueryParameterPresent(uri, "uid")
                || isQueryParameterPresent(uri, "mid")
                || isQueryParameterPresent(uri, "eid");
    }

    private boolean isQueryParameterPresent(Uri uri, String key) {
        if (uri != null && !TextUtils.isEmpty(key)) {
            String val = uri.getQueryParameter(key);
            return !TextUtils.isEmpty(val);
        }

        return false;
    }

    private boolean isShortUrlWithZ(Uri uri) {
        return uri != null && uri.getPath() != null && uri.getPath().startsWith("/z/");
    }

    private boolean isTrackUrl(Uri uri) {
        return uri != null && uri.getPath() != null && uri.getPath().startsWith("/track");
    }

    private void handleShortURL(final Uri uri) {
        BlueshiftLogger.d(TAG, "Short URL detected: " + uri);

        final Handler myHandler = getMyHandler();
        if (myHandler != null) {
            myHandler.post(invokeOnLinkProcessingStartRunnable());
            BlueshiftExecutor.getInstance().runOnNetworkThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Uri redir = replayUrl(uri);
                                if (redir != null) {
                                    myHandler.post(invokeOnLinkProcessingCompleteRunnable(redir));
                                } else {
                                    Exception e = new Exception("Unable to get redirection URL");
                                    myHandler.post(invokeOnLinkProcessingErrorRunnable(e));
                                }
                            } catch (Exception e) {
                                BlueshiftLogger.e(TAG, e);
                                myHandler.post(invokeOnLinkProcessingErrorRunnable(e));
                            }
                        }
                    }
            );
        }
    }

    private Uri replayUrl(Uri uri) throws Exception {
        Uri redir = null;
        if (uri != null) {
            String uriString = uri.toString();
            boolean isHttps = uriString.startsWith("https");
            if (isHttps) {
                HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(uriString).openConnection();
                // cache
                urlConnection.setUseCaches(true);
                urlConnection.setDefaultUseCaches(true);
                urlConnection.addRequestProperty("Cache-Control", "public");

                // replay
                urlConnection.setInstanceFollowRedirects(false);
                String location = urlConnection.getHeaderField("Location");
                if (TextUtils.isEmpty(location)) {
                    BlueshiftLogger.d(TAG, "No \'Location\' in response header");
                } else {
                    redir = Uri.parse(location);
                }

                // log response
                int responseCode = urlConnection.getResponseCode();
                BlueshiftLogger.d(TAG, "Response code: " + responseCode);
            } else {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(uriString).openConnection();
                // cache
                urlConnection.setUseCaches(true);
                urlConnection.setDefaultUseCaches(true);
                urlConnection.addRequestProperty("Cache-Control", "public");

                // replay
                urlConnection.setInstanceFollowRedirects(false);
                String location = urlConnection.getHeaderField("Location");
                if (TextUtils.isEmpty(location)) {
                    BlueshiftLogger.d(TAG, "No \'Location\' in response header");
                } else {
                    redir = Uri.parse(location);
                }

                // log response
                int responseCode = urlConnection.getResponseCode();
                BlueshiftLogger.d(TAG, "Response code: " + responseCode);
            }
        }

        return redir;
    }

    private Handler getMyHandler() {
        Handler handler = null;
        Looper looper = Looper.myLooper();
        if (looper != null) {
            handler = new Handler(looper);
        }
        return handler;
    }

    private Runnable invokeOnLinkProcessingStartRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                invokeOnLinkProcessingStart();
            }
        };
    }

    private Runnable invokeOnLinkProcessingCompleteRunnable(final Uri redirectionURL) {
        return new Runnable() {
            @Override
            public void run() {
                invokeOnLinkProcessingComplete(redirectionURL);
            }
        };
    }

    private Runnable invokeOnLinkProcessingErrorRunnable(final Exception e) {
        return new Runnable() {
            @Override
            public void run() {
                invokeOnLinkProcessingError(e);
            }
        };
    }

    private void handleTrackURL(Uri uri) {
        BlueshiftLogger.d(TAG, "Track URL detected: " + uri);

        invokeOnLinkProcessingStart();

        Uri redirectUri = null;

        if (uri != null) {
            String redirectUrl = uri.getQueryParameter(BlueshiftConstants.KEY_REDIR);
            try {
                if (TextUtils.isEmpty(redirectUrl)) {
                    BlueshiftLogger.d(TAG, "No redirect URL (redir) found in the given URL. " + uri);
                } else {
                    redirectUri = Uri.parse(redirectUrl);
                }
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);

                invokeOnLinkProcessingError(e);
                return;
            }
        }

        invokeOnLinkProcessingComplete(redirectUri);
    }

    private void invokeTrackAPICall(Uri uri) {
        BlueshiftLogger.d(TAG, "Fire /track api call for " + (uri != null ? uri : ""));

        Blueshift.getInstance(context).trackUniversalLinks(uri);
    }

    private void openUri(Uri uri) {
        try {
            if (uri != null) {
                openUri(context, uri, extras);
            } else {
                BlueshiftLogger.d(TAG, "Attempt to open app as NO Uri is available.");

                if (context != null) {
                    String pkgName = context.getPackageName();
                    Intent openAppIntent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
                    if (openAppIntent != null) {
                        if (extras != null) openAppIntent.putExtras(extras);
                        // Note: This will create a new task and launch the app with the corresponding
                        // activity. As per the docs, the dev should add parent activity to all the
                        // activities registered in the manifest in order to get the back stack working
                        // doc: https://developer.android.com/training/notify-user/navigation#DirectEntry
                        TaskStackBuilder.create(context).addNextIntentWithParentStack(openAppIntent).startActivities();
                    }
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void openUri(Context context, Uri data, Bundle extras) {
        BlueshiftLogger.d(TAG, "Attempt to open URL with Intent. Uri: " + data);

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(data);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            if (extras != null) intent.putExtras(extras);
            if (context != null) context.startActivity(intent);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    private void invokeOnLinkProcessingStart() {
        BlueshiftLogger.d(TAG, "invokeOnLinkProcessingStart()");
        if (listener != null) listener.onLinkProcessingStart();
    }

    private void invokeOnLinkProcessingComplete(Uri redirectionURL) {
        BlueshiftLogger.d(TAG, "invokeOnLinkProcessingComplete() > redirectionURL " + redirectionURL);
        if (listener != null) {
            listener.onLinkProcessingComplete(redirectionURL);
        } else {
            // if listener is not present, SDK will attempt to open the URL
            openUri(redirectionURL);
        }
    }

    private void invokeOnLinkProcessingError(Exception e) {
        BlueshiftLogger.d(TAG, "invokeOnLinkProcessingError()");
        if (listener != null) listener.onLinkProcessingError(e);
    }

    private static class ReplayBlueshiftShortLinkTask extends AsyncTask<Uri, Void, Uri> {

        BlueshiftLinksListener listener;

        ReplayBlueshiftShortLinkTask(BlueshiftLinksListener listener) {
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            if (listener != null) listener.onLinkProcessingStart();
        }

        @Override
        protected Uri doInBackground(Uri... uris) {
            Uri uri = uris != null && uris.length > 0 ? uris[0] : null;
            if (uri == null || TextUtils.isEmpty(uri.toString())) return null;

            boolean isHttps = uri.toString().startsWith("https");
            return isHttps ? replayHttpsUrl(uri) : replayHttpUrl(uri);
        }

        private Uri replayHttpUrl(Uri source) {
            HttpURLConnection urlConnection = null;
            Uri redirUri = null;

            try {
                BlueshiftLogger.d(TAG, "(http) Requesting for redirection URL");
                long start = System.currentTimeMillis();

                urlConnection = (HttpURLConnection) openConnection(source.toString());
                redirUri = replayUrl(urlConnection);

                long diff = System.currentTimeMillis() - start;
                BlueshiftLogger.d(TAG, "Redirection URL request complete in " + diff + " ms");
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

            return redirUri;
        }

        private Uri replayHttpsUrl(Uri source) {
            HttpsURLConnection urlConnection = null;
            Uri redirUri = null;

            try {
                BlueshiftLogger.d(TAG, "(https) Requesting for redirection URL");
                long start = System.currentTimeMillis();

                urlConnection = (HttpsURLConnection) openConnection(source.toString());
                redirUri = replayUrl(urlConnection);

                long diff = System.currentTimeMillis() - start;
                BlueshiftLogger.d(TAG, "Redirection URL request complete in " + diff + " ms");
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

            return redirUri;
        }

        private URLConnection openConnection(String reqURL) throws IOException {
            URL url = new URL(reqURL);
            return url.openConnection();
        }

        private Uri replayUrl(HttpURLConnection urlConnection) {
            addRequestProperties(urlConnection);
            logResponseStatusCode(urlConnection);
            return getRedirectionUrl(urlConnection);
        }

        private void addRequestProperties(HttpURLConnection urlConnection) {
            if (urlConnection != null) {
                urlConnection.setUseCaches(true);
                urlConnection.setDefaultUseCaches(true);
                urlConnection.addRequestProperty("Cache-Control", "public");
            }
        }

        private void logResponseStatusCode(HttpURLConnection urlConnection) {
            if (urlConnection != null) {
                try {
                    BlueshiftLogger.d(TAG, "Response code: " + urlConnection.getResponseCode());
                } catch (IOException e) {
                    BlueshiftLogger.e(TAG, e);
                }
            }
        }

        private Uri getRedirectionUrl(HttpURLConnection urlConnection) {
            Uri uri = null;

            if (urlConnection != null) {
                String location = urlConnection.getHeaderField("Location");
                if (TextUtils.isEmpty(location)) {
                    BlueshiftLogger.d(TAG, "No \'Location\' in response header");
                } else {
                    uri = Uri.parse(location);
                    BlueshiftLogger.d(TAG, "Location: " + location);
                }
            }

            return uri;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (listener != null) {
                if (uri != null) {
                    listener.onLinkProcessingComplete(uri);
                } else {
                    listener.onLinkProcessingError(new Exception("Could not get redirectionURL from shortURL"));
                }
            }
        }
    }
}
