package com.blueshift;

import android.util.Base64;

import com.blueshift.model.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class BlueshiftHttpClient {
    private static final String TAG = "BlueshiftHttpClient";
    private static BlueshiftHttpClient instance = null;
    private static final Map<String, String> headers = new HashMap<>();

    private BlueshiftHttpClient() {
    }

    public static BlueshiftHttpClient getInstance() {
        if (instance == null) instance = new BlueshiftHttpClient();
        return instance;
    }

    public void init(Configuration configuration) {
        synchronized (headers) {
            String eventApiKey = configuration != null ? configuration.getApiKey() : null;
            if (eventApiKey != null && !eventApiKey.equals("")) {
                // SDK is initialized with a non-empty api key
                String credString = eventApiKey + ":";
                String base64 = Base64.encodeToString(credString.getBytes(), Base64.DEFAULT);
                String sanitizedBase64 = base64.replace("\n", "");
                headers.put("Authorization", "Basic " + sanitizedBase64);
            }

            headers.put("Connection", "close");
        }
    }


    private HttpsURLConnection openConnection(String url) throws IOException {
        synchronized (headers) {
            HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(url).openConnection();
            // append headers
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                urlConnection.addRequestProperty(entry.getKey(), entry.getValue());
            }

            return urlConnection;
        }
    }

    public void post(String url, String json) {
        if (url != null) {
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = openConnection(url);
                // Headers
                urlConnection.addRequestProperty("Accept", "application/json");
                // Method
                urlConnection.setRequestMethod("POST");
                // Write Body
                urlConnection.setDoOutput(true);
                OutputStream outputStream = null;
                try {
                    if (json != null) {
                        outputStream = urlConnection.getOutputStream();
                        outputStream.write(json.getBytes());
                        outputStream.close();
                    }
                } catch (IOException e) {
                    BlueshiftLogger.e(TAG, e);
                } finally {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }
                // Write response code
                int responseCode = urlConnection.getResponseCode();
                // Write response body
                String responseBody = readResponseBody(urlConnection);

                String builder = "{ " +
                        "\"URL\" : \"" + url + "\", " +
                        "\"Method\" : \"" + "POST" + "\", " +
                        "\"Params\" : " + json + ", " +
                        "\"Status\" : " + responseCode + ", " +
                        "\"Response\" : " + responseBody +
                        " }";
                BlueshiftLogger.d(TAG, builder);
            } catch (IOException e) {
                BlueshiftLogger.e(TAG, e);
            } finally {
                if (urlConnection != null) {
                    try {
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        BlueshiftLogger.e(TAG, e);
                    }
                }
            }
        }
    }

    private String readResponseBody(HttpsURLConnection urlConnection) {
        try {
            return readStream(urlConnection.getInputStream());
        } catch (IOException e) {
            try {
                return readStream(urlConnection.getErrorStream());
            } catch (IOException ex) {
                BlueshiftLogger.e(TAG, ex);
            }

            BlueshiftLogger.e(TAG, e);
        }

        return "";
    }

    private String readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int c = inputStream.read();
        while (c != -1) {
            byteArrayOutputStream.write(c);
            c = inputStream.read();
        }

        return byteArrayOutputStream.toString();
    }
}
