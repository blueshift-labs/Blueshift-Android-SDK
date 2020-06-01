package com.blueshift;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import java.net.HttpURLConnection;
import java.net.URL;

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

    public static boolean isBlueshiftLink(Uri uri) {
        return isQueryParameterPresent(uri, "uid") && isQueryParameterPresent(uri, "mid");
    }

    private static boolean isQueryParameterPresent(Uri uri, String key) {
        if (uri != null && !TextUtils.isEmpty(key)) {
            String val = uri.getQueryParameter(key);
            return !TextUtils.isEmpty(val);
        }

        return false;
    }

    private boolean hasBlueshiftParamsInURL(Uri uri) {
        return isBlueshiftLink(uri) || isQueryParameterPresent(uri, "eid");
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
                                    myHandler.post(invokeOnLinkProcessingErrorRunnable(e, uri));
                                }
                            } catch (Exception e) {
                                BlueshiftLogger.e(TAG, e);
                                myHandler.post(invokeOnLinkProcessingErrorRunnable(e, uri));
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

    private Runnable invokeOnLinkProcessingCompleteRunnable(final Uri link) {
        return new Runnable() {
            @Override
            public void run() {
                invokeOnLinkProcessingComplete(link);
            }
        };
    }

    private Runnable invokeOnLinkProcessingErrorRunnable(final Exception e, final Uri link) {
        return new Runnable() {
            @Override
            public void run() {
                invokeOnLinkProcessingError(e, link);
            }
        };
    }

    private void handleTrackURL(Uri uri) {
        BlueshiftLogger.d(TAG, "Track URL detected: " + uri);

        invokeOnLinkProcessingStart();

        if (uri != null) {
            String redirUrl = uri.getQueryParameter(BlueshiftConstants.KEY_REDIR);
            try {
                if (TextUtils.isEmpty(redirUrl)) {
                    Exception e = new Exception("No redirect URL (redir) found in the given URL." + uri);
                    BlueshiftLogger.e(TAG, e);

                    invokeOnLinkProcessingError(e, uri);
                } else {
                    Uri redir = Uri.parse(redirUrl);
                    invokeOnLinkProcessingComplete(redir);
                }
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);

                invokeOnLinkProcessingError(e, uri);
            }
        }
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

    private void invokeOnLinkProcessingError(Exception e, Uri link) {
        BlueshiftLogger.d(TAG, "invokeOnLinkProcessingError()");
        if (listener != null) listener.onLinkProcessingError(e, link);
    }
}
