package com.blueshift.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.blueshift.BlueshiftLogger;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

/**
 * @author Rahul Raveendran V P
 *         Created on 19/2/15 @ 3:04 PM
 *         https://github.com/rahulrvp
 */
public class NetworkUtils {

    public static boolean downloadFile(String url, String destinationPath) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        HttpsURLConnection httpsURLConnection = null;
        try {
            URL downloadURL = new URL(url);
            httpsURLConnection = (HttpsURLConnection) downloadURL.openConnection();
            httpsURLConnection.connect();

            if (httpsURLConnection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                return false;
            }

            inputStream = httpsURLConnection.getInputStream();
            outputStream = new FileOutputStream(destinationPath);

            byte[] data = new byte[4096];
            int count = 0;

            while ((count = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, count);
            }

            // clean up memory
            outputStream.close();
            inputStream.close();
            httpsURLConnection.disconnect();
        } catch (Exception e) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ex) {
                BlueshiftLogger.e(null, e);
            }

            if (httpsURLConnection != null) {
                httpsURLConnection.disconnect();
            }

            return false;
        }

        return true;
    }

    public static boolean isConnected(Context context) {
        boolean hasNetwork = false;

        if (context != null &&
                PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {

            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager != null) {
                @SuppressLint("MissingPermission")
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                hasNetwork = networkInfo != null && networkInfo.isConnectedOrConnecting();
            }
        }

        return hasNetwork;
    }

    public static String encodeUrlParam(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (Exception e) {
            return input;
        }
    }
}
