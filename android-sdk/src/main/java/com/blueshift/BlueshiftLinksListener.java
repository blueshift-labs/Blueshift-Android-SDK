package com.blueshift;

import android.net.Uri;

public interface BlueshiftLinksListener {
    void onLinkProcessingStart();

    void onLinkProcessingComplete(Uri link);

    void onLinkProcessingError(Exception e, Uri link);
}
