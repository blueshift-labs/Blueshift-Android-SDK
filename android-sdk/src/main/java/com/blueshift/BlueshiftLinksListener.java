package com.blueshift;

import android.net.Uri;

public interface BlueshiftLinksListener {
    void onLinkProcessingStart();

    void onLinkProcessingComplete(Uri redirectionURL);

    void onLinkProcessingError(Exception e);
}
