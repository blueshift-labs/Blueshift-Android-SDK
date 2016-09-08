package com.blueshift.rich_push;

/**
 * Created by rahul on 6/9/16.
 */
public enum NotificationType {
    AlertDialog,
    Notification,
    Unknown;

    public static NotificationType fromString(String notificationType) {
        if (notificationType != null) {
            switch (notificationType) {
                case "alert":
                    return AlertDialog;

                case "notification":
                    return Notification;

                default:
                    return Unknown;
            }
        } else {
            return Unknown;
        }
    }
}
