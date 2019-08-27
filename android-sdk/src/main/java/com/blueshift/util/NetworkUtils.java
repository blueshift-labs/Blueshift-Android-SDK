package com.blueshift.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Rahul Raveendran V P
 *         Created on 19/2/15 @ 3:04 PM
 *         https://github.com/rahulrvp
 */
public class NetworkUtils {

    public static boolean downloadFile(String url, String destinationPath) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL downloadURL = new URL(url);
            httpURLConnection = (HttpURLConnection) downloadURL.openConnection();
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            inputStream = httpURLConnection.getInputStream();
            outputStream = new FileOutputStream(destinationPath);

            byte[] data = new byte[4096];
            int count = 0;

            while ((count = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, count);
            }

            // clean up memory
            outputStream.close();
            inputStream.close();
            httpURLConnection.disconnect();
        } catch (Exception e) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
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
}
