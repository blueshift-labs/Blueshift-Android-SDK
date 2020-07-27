package com.blueshift;

public class BlueshiftHttpResponse {
    private int code;
    private String body;

    public int getCode() {
        return code;
    }

    public String getBody() {
        return body;
    }

    public static class Builder {
        private int code;
        private String body;

        public void setCode(int code) {
            this.code = code;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public BlueshiftHttpResponse build() {
            BlueshiftHttpResponse response = new BlueshiftHttpResponse();
            response.code = code;
            response.body = body;
            return response;
        }
    }
}
