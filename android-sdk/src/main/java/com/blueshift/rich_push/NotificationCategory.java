package com.blueshift.rich_push;

import com.blueshift.BlueshiftLogger;

/**
 * @author Rahul Raveendran V P
 *         Created on 6/9/16 @ 12:22 PM
 *         https://github.com/rahulrvp
 */
public enum NotificationCategory {
    SilentPush,
    AnimatedCarousel,
    Carousel,
    GifNotification,
    Unknown;

    private static final String LOG_TAG = "NotificationCategory";

    public static NotificationCategory fromString(String notificationCategory) {
        if (notificationCategory != null) {
            switch (notificationCategory) {
                // silent push
                case "silent_push":
                    return SilentPush;

                // custom notifications
                case "animated_carousel":
                    return AnimatedCarousel;

                case "carousel":
                    return Carousel;

                case "gif":
                    return GifNotification;

                default:
                    BlueshiftLogger.w(LOG_TAG, "Unknown 'category' found: " + notificationCategory);

                    return Unknown;
            }
        } else {
            BlueshiftLogger.w(LOG_TAG, "'category' is not available inside 'message'.");

            return Unknown;
        }
    }
}
