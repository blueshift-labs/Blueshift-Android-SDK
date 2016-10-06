package com.blueshift.util;

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

            if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK ) {
                return false;
            }

            inputStream = httpURLConnection.getInputStream();
            outputStream = new FileOutputStream(destinationPath);

            byte data[] = new byte[4096];
            int count = 0;

            while ((count = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, count);
            }
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
}
