package com.blueshift.inappmessage;

public interface InAppApiCallback {
    void onSuccess();

    void onFailure(int code, String message);
}
