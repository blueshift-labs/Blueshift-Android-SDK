package com.blueshift.rich_push;

import com.blueshift.BlueshiftLogger;

/**
 * @author Rahul Raveendran V P
 *         Created on 6/9/16 @ 3:03 PM
 *         https://github.com/rahulrvp
 */
public enum NotificationType {
    Notification,
    CustomNotification,
    NotificationScheduler,
    Unknown;

    private static final String LOG_TAG = "NotificationType";

    public static NotificationType fromString(String notificationType) {
        if (notificationType != null) {
            switch (notificationType) {
                case "notification":
                    return Notification;

                case "custom_notification":
                    return CustomNotification;

                case "notification_scheduler":
                    return NotificationScheduler;

                default:
                    BlueshiftLogger.w(LOG_TAG, "Unknown 'notification_type' found: " + notificationType);

                    return Unknown;
            }
        } else {
            BlueshiftLogger.w(LOG_TAG, "'notification_type' is not available inside 'message'.");

            return Unknown;
        }
    }
}
