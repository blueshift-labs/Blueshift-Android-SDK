package com.blueshift;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

    public void handleBlueshiftUniversalLinks(Intent intent, BlueshiftLinksListener listener) {
        if (intent != null) {
            handleBlueshiftUniversalLinks(intent.getData(), intent.getExtras(), listener);
        }
    }

    public void handleBlueshiftUniversalLinks(Uri data, Bundle extras, BlueshiftLinksListener listener) {
        if (data == null) return;

        this.extras = extras;
        this.listener = listener;

        if (isShortUrlWithZ(data)) {
            handleShortURL(data);
        } else if (isTrackUrl(data)) {
            invokeTrackAPICall(data);
            handleTrackURL(data);
        } else {
            BlueshiftLogger.d(TAG, "Non-Blueshift URL detected: " + data);
            invokeOnLinkProcessingComplete(data);
        }
    }

    private boolean isShortUrlWithZ(Uri uri) {
        return uri != null && uri.getPath() != null && uri.getPath().startsWith("/z/");
    }

    private boolean isTrackUrl(Uri uri) {
        return uri != null && uri.getPath() != null && uri.getPath().startsWith("/track");
    }

    private void handleShortURL(Uri uri) {
        BlueshiftLogger.d(TAG, "Short URL detected: " + uri);

        new ReplayBlueshiftShortLinkTask(new BlueshiftLinksListener() {
            @Override
            public void onLinkProcessingStart() {
                invokeOnLinkProcessingStart();
            }

            @Override
            public void onLinkProcessingComplete(Uri redirectionURL) {
                invokeTrackAPICall(redirectionURL);
                invokeOnLinkProcessingComplete(redirectionURL);
            }

            @Override
            public void onLinkProcessingError(Exception e) {
                invokeOnLinkProcessingError(e);
            }
        }).execute(uri);
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
            HttpURLConnection httpURLConnection = null;
            Uri redirUri = null;

            try {
                BlueshiftLogger.d(TAG, "Requesting for redirection URL");
                long start = System.currentTimeMillis();

                URL url = new URL(source.toString());
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setInstanceFollowRedirects(false);
                String location = httpURLConnection.getHeaderField("Location");
                if (TextUtils.isEmpty(location)) {
                    BlueshiftLogger.d(TAG, "No \'Location\' in response header");
                } else {
                    redirUri = Uri.parse(location);
                    BlueshiftLogger.d(TAG, "Location: " + redirUri);
                }

                long diff = System.currentTimeMillis() - start;
                BlueshiftLogger.d(TAG, "Redirection URL request complete in " + diff + " ms");
            } catch (MalformedURLException e) {
                BlueshiftLogger.e(TAG, e);
            } catch (IOException e) {
                BlueshiftLogger.e(TAG, e);
            } finally {
                if (httpURLConnection != null) httpURLConnection.disconnect();
            }

            return redirUri;
        }

        private Uri replayHttpsUrl(Uri source) {
            HttpsURLConnection httpsURLConnection = null;
            Uri redirUri = null;

            try {
                BlueshiftLogger.d(TAG, "Requesting for redirection URL");
                long start = System.currentTimeMillis();

                URL url = new URL(source.toString());
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
                httpsURLConnection.setInstanceFollowRedirects(false);
                String location = httpsURLConnection.getHeaderField("Location");
                if (TextUtils.isEmpty(location)) {
                    BlueshiftLogger.d(TAG, "No \'Location\' in response header");
                } else {
                    redirUri = Uri.parse(location);
                    BlueshiftLogger.d(TAG, "Location: " + redirUri);
                }

                long diff = System.currentTimeMillis() - start;
                BlueshiftLogger.d(TAG, "Redirection URL request complete in " + diff + " ms");
            } catch (MalformedURLException e) {
                BlueshiftLogger.e(TAG, e);
            } catch (IOException e) {
                BlueshiftLogger.e(TAG, e);
            } finally {
                if (httpsURLConnection != null) httpsURLConnection.disconnect();
            }

            return redirUri;
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
