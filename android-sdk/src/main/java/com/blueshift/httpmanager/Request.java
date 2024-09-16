package com.blueshift.httpmanager;

import com.blueshift.BlueshiftLogger;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 25/2/15 @ 3:04 PM
 *         https://github.com/rahulrvp
 *
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
public class Request implements Serializable {
    private long id;
    private String url;
    private Method method;
    private boolean multipart;
    private String entityType;
    private long entityID;
    private String requestType;
    private String filePath;
    private String paramJson;
    private HashMap<String, String> urlParams;
    private int pendingRetryCount;
    private long nextRetryTime;

    public Request() {
        pendingRetryCount = 1;
        nextRetryTime = 0;
    }

    public void log(String tag, Response response) {
        String builder = "{" +
                "\"url\":\"" + url + "\"," +
                "\"method\":\"" + method + "\"," +
                "\"params\":" + paramJson + "," +
                "\"status\":" + (response != null ? response.getStatusCode() : "\"NA\"") + "," +
                "\"response\":" + (response != null ? response.getResponseBody() : "\"NA\"") +
                "}";
        BlueshiftLogger.d(tag, builder);
    }

    public HashMap<String, String> getUrlParams() {
        return urlParams;
    }

    public String getUrlParamsAsJSON() {
        return new Gson().toJson(urlParams);
    }

    public void setUrlParams(HashMap<String, String> urlParams) {
        this.urlParams = urlParams;
    }

    public void setUrlParams(String paramJson) {
        this.urlParams = new Gson().fromJson(paramJson, new HashMap<String, String>().getClass());
    }

    public String getParamJson() {
        return paramJson;
    }

    public void setParamJson(String paramJson) {
        this.paramJson = paramJson;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public long getEntityID() {
        return entityID;
    }

    public void setEntityID(long entityID) {
        this.entityID = entityID;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPendingRetryCount() {
        return pendingRetryCount;
    }

    public void setPendingRetryCount(int pendingRetryCount) {
        this.pendingRetryCount = pendingRetryCount;
    }

    public long getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(long nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }
}
