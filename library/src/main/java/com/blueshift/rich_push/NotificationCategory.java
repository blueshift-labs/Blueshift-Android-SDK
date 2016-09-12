package com.blueshift.rich_push;

import android.util.Log;

/**
 * Created by rahul on 6/9/16.
 */
public enum NotificationCategory {
    Buy,
    ViewCart,
    Promotion,
    AlertBoxOpenDismiss,
    AlertBoxDismiss,
    SilentPush,
    Unknown;

    public static NotificationCategory fromString(String notificationCategory) {
        if (notificationCategory != null) {
            switch (notificationCategory) {
                case "buy":
                    return Buy;

                case "view cart":
                    return ViewCart;

                case "promotion":
                    return Promotion;

                case "alert_box":
                    return AlertBoxOpenDismiss;

                case "alert_box_1_button":
                    return AlertBoxDismiss;

                case "silent_push":
                    return SilentPush;

                default:
                    Log.w("NotificationCategory", "Unknown notification_type found: " + notificationCategory);

                    return Unknown;
            }
        } else {
            return Unknown;
        }
    }

    public int getNotificationId() {
        switch (this) {
            case Buy:
                return 100;

            case ViewCart:
                return 200;

            case Promotion:
                return 300;

            case AlertBoxOpenDismiss:
                return 400;

            case AlertBoxDismiss:
                return 500;

            default:
                return 0;
        }
    }
}