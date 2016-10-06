package com.blueshift.rich_push;

import android.util.Log;

/**
 * @author Rahul Raveendran V P
 *         Created on 6/9/16 @ 3:03 PM
 *         https://github.com/rahulrvp
 */
public enum NotificationType {
    AlertDialog,
    Notification,
    CustomNotification,
    Unknown;

    private static final String LOG_TAG = "NotificationType";

    public static NotificationType fromString(String notificationType) {
        if (notificationType != null) {
            switch (notificationType) {
                case "alert":
                    return AlertDialog;

                case "notification":
                    return Notification;

                case "custom_notification":
                    return CustomNotification;

                default:
                    Log.w(LOG_TAG, "Unknown 'notification_type' found: " + notificationType);

                    return Unknown;
            }
        } else {
            Log.w(LOG_TAG, "'notification_type' is not available inside 'message'.");

            return Unknown;
        }
    }
}
