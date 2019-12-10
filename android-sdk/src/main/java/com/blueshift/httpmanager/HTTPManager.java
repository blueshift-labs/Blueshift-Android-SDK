package com.blueshift.httpmanager;

import androidx.annotation.NonNull;
import android.util.Base64;

import com.blueshift.util.SdkLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Abstraction for TCP/IP communication.
 *
 * Author : Asif CH
 */

public class HTTPManager {
    private static final String LOG_TAG = "BfHttpManager";

    private String mUrl;
    private HttpURLConnection mUrlConnection;
    private HashMap<String, String> mRequestProperties;
    private boolean mIgnoreHostnameVerification;
    private String mAuthToken;

    public static final int HTTP_OK = 200;
    public static final int HTTP_FILE_NOT_FOUND = 404;
    public static final int HTTP_SERVER_ERROR = 500;
    private static final String boundary = "000boundary000";

    public HTTPManager(String url) {
        mUrl = url;
        mRequestProperties = new HashMap<String, String>();
        mIgnoreHostnameVerification = false;
        mAuthToken = null;

        addRequestProperty("User-Agent", System.getProperty("http.agent"));
        addRequestProperty("Accept", "application/json");
        addRequestProperty("Connection", "close");
    }

    public void addBasicAuthentication(@NonNull String username, @NonNull String password) {
        String credentials = username + ":" + password;
        String credBase64 = Base64.encodeToString(credentials.getBytes(), Base64.DEFAULT).replace("\n", "");
        addRequestProperty("Authorization", "Basic " + credBase64);
    }

    public HTTPManager addRequestProperty(String key, String value) {
        mRequestProperties.put(key, value);
        return this;
    }

    public HTTPManager ignoreHostnameVerification() {
        mIgnoreHostnameVerification = true;
        return this;
    }

    public HTTPManager setCookie(String cookie) {
        addRequestProperty("Cookie", cookie);
        return this;
    }

    public HTTPManager setAuthToken(String authToken) {
        mAuthToken = authToken;
        return this;
    }

    private void prepareRequest() {
        URL urlObject = null;
        try {
            urlObject = new URL(mUrl);
            mUrlConnection = (HttpURLConnection) urlObject.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //try { mUrlConnection.setDoOutput(true); } catch (Exception e) {}
        //try { mUrlConnection.setDoInput(true); } catch (Exception e) {}

        for (String key : mRequestProperties.keySet()) {
            mUrlConnection.addRequestProperty(key, mRequestProperties.get(key));
        }

        if (mIgnoreHostnameVerification) {
            trustAllHosts();
            ((HttpsURLConnection)mUrlConnection).setHostnameVerifier(DO_NOT_VERIFY);
        }
    }

    private void setUrlEncoded() {
        mUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
    }

    private void setRequestMethod(String method) {
        try {
            mUrlConnection.setRequestMethod(method);
        } catch (ProtocolException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * generate argument string to append to url
     *
     * @param params map of get/post body
     * @return argument string to append to url
     */
    private String getUrlParams(final HashMap<String, String> params) {
        StringBuilder bodyBuilder = new StringBuilder();

        if (mAuthToken != null) {
            bodyBuilder.append("authentication_token").append('=').append(mAuthToken).append("&");
        }

        if (params != null) {
            Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> param = iterator.next();
                bodyBuilder.append(param.getKey()).append('=').append(param.getValue());
                if (iterator.hasNext()) {
                    bodyBuilder.append('&');
                }
            }
        }

        return bodyBuilder.toString();
    }

    private void writeBody(String jsonData) {
        if (mAuthToken != null) {
            mUrl = mUrl + "?authentication_token=" + mAuthToken;
        }

        mUrlConnection.setFixedLengthStreamingMode(jsonData.getBytes().length);

        OutputStream outputStream = null;
        try {
            outputStream = mUrlConnection.getOutputStream();
            outputStream.write(jsonData.getBytes());
            outputStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private Response getResponse() {
        Response response = new Response();
        SdkLog.d(LOG_TAG, mUrlConnection.getRequestMethod() + " " + mUrl);

        try {
            response.setStatusCode(mUrlConnection.getResponseCode());
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("authentication challenge")) {
                try {
                    response.setStatusCode(mUrlConnection.getResponseCode());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        response.setResponseHeaders(mUrlConnection.getHeaderFields());

        try {
            response.setResponseBody(readStream(mUrlConnection.getInputStream()));
        } catch (Exception e) {
            response.setResponseBody(readStream(mUrlConnection.getErrorStream()));
        } finally {
            if (mUrlConnection != null) {
                mUrlConnection.disconnect();
            }
        }

        return response;
    }

    public Response get() {
        if (mAuthToken != null) {
            mUrl = mUrl + "?authentication_token=" + mAuthToken;
        }
        prepareRequest();

        setRequestMethod("GET");
        setUrlEncoded();

        return getResponse();
    }

    public Response get(HashMap<String, String> params) {
        String urlParams = getUrlParams(params);
        mUrl = mUrl + "?" + urlParams;
        prepareRequest();

        setRequestMethod("GET");
        setUrlEncoded();

        return getResponse();
    }

    public Response post(HashMap<String, String> params) {
        String urlParams = getUrlParams(params);
        mUrl = mUrl + "?" + urlParams;
        prepareRequest();

        setRequestMethod("POST");
        setUrlEncoded();

        return getResponse();
    }

    public Response post(String paramsJson) {
        prepareRequest();

        mUrlConnection.setDoOutput(true);
        mUrlConnection.setDoInput(true);

        setRequestMethod("POST");
        mUrlConnection.setRequestProperty("Content-Type", "application/json");

        writeBody(paramsJson);

        return getResponse();
    }

    public Response put(HashMap<String, String> params) {
        String urlParams = getUrlParams(params);
        mUrl = mUrl + urlParams;
        prepareRequest();

        setRequestMethod("PUT");
        setUrlEncoded();

        return getResponse();
    }

    public Response put(String paramsJson) {
        prepareRequest();

        setRequestMethod("PUT");
        mUrlConnection.setRequestProperty("Content-Type", "application/json");

        writeBody(paramsJson);

        return getResponse();
    }

    public Response delete() {
        prepareRequest();

        setRequestMethod("DELETE");
        //setUrlEncoded();

        return getResponse();
    }

    public Response delete(HashMap<String, String> params) {
        String urlParams = getUrlParams(params);
        mUrl = mUrl + "?" + urlParams;
        prepareRequest();

        setRequestMethod("DELETE");
        //setUrlEncoded();

        return getResponse();
    }

    /**
     * read response text from http response stream
     *
     * @param inputStream http call response stream
     * @return http response string
     */
    private String readStream(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int c = inputStream.read();
            while (c != -1) {
                byteArrayOutputStream.write(c);
                c = inputStream.read();
            }
            return byteArrayOutputStream.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * The below code is used to trust the https connection
     * without verifying the hostname. This is used for testing
     * purpose only, in development mode.
     */

    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
