package com.blueshift.httpmanager;

import java.util.List;
import java.util.Map;

/**
 * @author Rahul Raveendran V P
 *         Created on 05/12/13 @ 3:04 PM
 *         https://github.com/rahulrvp
 *
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
public class Response {
    private int statusCode; // HTTP Status Code
    private String responseBody;
    private Map<String, List<String>> responseHeaders;

    public boolean isSuccess() {
        return statusCode / 100 == 2;
    }

    public boolean isUnAuthorized() {
        return statusCode == 401;
    }

    public int getStatusCode() {
        return statusCode;
    }

    protected void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    protected void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }
}
