package com.blueshift;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public class BlueshiftHttpManager {
    private static final String TAG = "BlueshiftHttpManager";
    private static final Boolean lock = true;
    private static BlueshiftHttpManager instance = null;

    private BlueshiftHttpManager() {
    }

    public static BlueshiftHttpManager getInstance() {
        synchronized (lock) {
            if (instance == null) instance = new BlueshiftHttpManager();
            return instance;
        }
    }

    public BlueshiftHttpResponse send(BlueshiftHttpRequest request) {
        synchronized (lock) {
            BlueshiftHttpResponse.Builder builder = new BlueshiftHttpResponse.Builder();

            if (request != null) {
                HttpsURLConnection connection = null;
                try {
                    // open connection
                    connection = openConnection(request.getUrlWithParams());

                    // set method
                    connection.setRequestMethod(request.getMethod().name());

                    // add req headers
                    connection = addHeaders(connection, request.getReqHeaderJson());

                    // write body
                    connection = writeReqBody(connection, request.getReqBodyJson());

                    // get response code
                    int responseCode = connection.getResponseCode();
                    builder.setCode(responseCode);

                    // get response body
                    String responseBody = readResponseBody(connection);
                    builder.setBody(responseBody);

                    connection.disconnect();
                } catch (Exception e) {
                    BlueshiftLogger.e(TAG, e);
                } finally {
                    if (connection != null) {
                        try {
                            connection.disconnect();
                        } catch (Exception ignored) {

                        }
                    }
                }
            }

            BlueshiftHttpResponse response = builder.build();

            // log request details for debugging purposes
            logRequest(request);
            logResponse(response);

            return response;
        }
    }

    private void logRequest(BlueshiftHttpRequest request) {
        if (request != null) {
            try {
                String log = "{" +
                        "\"api\":\"" + request.getUrlWithParams() + "\"," +
                        "\"method\":\"" + request.getMethod() + "\"," +
                        "\"body\":" + request.getReqBodyJson() +
                        "}";

                BlueshiftLogger.i(TAG, log);
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            }
        }
    }

    private void logResponse(BlueshiftHttpResponse response) {
        if (response != null) {
            JSONObject logger = new JSONObject();
            try {
                logger.putOpt("status", response.getCode());
            } catch (Exception ignored) {
            }

            try {
                logger.putOpt("response", new JSONObject(response.getBody()));
            } catch (Exception e) {
                try {
                    logger.putOpt("response", response.getBody());
                } catch (Exception ignored) {
                }
            }

            BlueshiftLogger.i(TAG, logger.toString());
        }
    }

    private HttpsURLConnection openConnection(String url) throws IOException {
        return (HttpsURLConnection) new URL(url).openConnection();
    }

    private HttpsURLConnection addHeaders(HttpsURLConnection connection, JSONObject headerJson) {
        if (connection != null) {
            // add common headers

            // add provided headers
            if (headerJson != null) {
                Iterator<String> keys = headerJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = headerJson.opt(key);
                    connection.addRequestProperty(key, String.valueOf(val));
                }
            }
        }

        return connection;
    }

    private HttpsURLConnection writeReqBody(HttpsURLConnection urlConnection, JSONObject reqBodyJson) throws IOException {
        if (urlConnection != null && reqBodyJson != null && reqBodyJson.keys().hasNext()) {
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);

            OutputStream outputStream = null;
            try {
                outputStream = urlConnection.getOutputStream();

                OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                writer.write(reqBodyJson.toString());
                writer.flush();
                writer.close();

                outputStream.close();
            } catch (Exception e) {
                BlueshiftLogger.e(TAG, e);
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        }

        return urlConnection;
    }

    private String readResponseBody(HttpsURLConnection urlConnection) {
        try {
            return readStream(urlConnection.getInputStream());
        } catch (Exception e) {
            try {
                return readStream(urlConnection.getErrorStream());
            } catch (Exception ex) {
                BlueshiftLogger.e(TAG, ex);
            }

            BlueshiftLogger.e(TAG, e);
        }

        return "";
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder buffer = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(reader);

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            buffer.append(line);
        }

        return buffer.toString();
    }
}
