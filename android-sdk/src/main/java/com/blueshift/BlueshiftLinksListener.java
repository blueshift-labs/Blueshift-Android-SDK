package com.blueshift;

import android.net.Uri;

public interface BlueshiftLinksListener {
    /**
     * Invoked before processing the app-links
     */
    void onLinkProcessingStart();

    /**
     * Invoked when the app-link processing is complete.
     *
     * Non-Blueshift links will also be passed into the host app using
     * this callback method.
     *
     * @param link The redirection URL obtained from the app-link
     */
    void onLinkProcessingComplete(Uri link);

    /**
     * Invoked when the app-link processing fails due to API failure or
     * when the redir URL is unavailable. Original URL will be passed
     * into the host app in this case.
     *
     * @param e    {@link Exception} object with a valid error message
     * @param link Original link passes into the SDK for processing
     */
    void onLinkProcessingError(Exception e, Uri link);
}
