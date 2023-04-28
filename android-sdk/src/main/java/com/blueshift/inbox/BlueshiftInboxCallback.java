package com.blueshift.inbox;

public interface BlueshiftInboxCallback<T> {
    void onComplete(T t);
}
