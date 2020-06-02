package com.blueshift.rich_push;

import android.util.Log;

import com.blueshift.BlueshiftLogger;

/**
 * @author Rahul Raveendran V P
 *         Created on 6/9/16 @ 12:22 PM
 *         https://github.com/rahulrvp
 */
public enum NotificationCategory {
    Buy,
    ViewCart,
    Promotion,
    AlertBoxOpenDismiss,
    AlertBoxDismiss,
    SilentPush,
    AnimatedCarousel,
    Carousel,
    GifNotification,
    Unknown;

    private static final String LOG_TAG = "NotificationCategory";

    public static NotificationCategory fromString(String notificationCategory) {
        if (notificationCategory != null) {
            switch (notificationCategory) {
                // for regular notifications
                case "buy":
                    return Buy;

                case "view_cart":
                    return ViewCart;

                case "promotion":
                    return Promotion;

                // for dialog notifications
                case "alert_box":
                    return AlertBoxOpenDismiss;

                case "alert_box_1_button":
                    return AlertBoxDismiss;

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
            Log.w(LOG_TAG, "'category' is not available inside 'message'.");

            return Unknown;
        }
    }
}
