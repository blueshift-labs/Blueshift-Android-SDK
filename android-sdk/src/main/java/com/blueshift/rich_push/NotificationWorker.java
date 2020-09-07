package com.blueshift.rich_push;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.blueshift.util.NotificationUtils;

/**
 * @author Rahul Raveendran V P
 *         Created on 20/9/16 @ 12:00 PM @ 3:04 PM
 *         https://github.com/rahulrvp
 */
public class NotificationWorker extends IntentService {

    public static final String ACTION_CAROUSEL_IMG_CHANGE = "carousel_image_change";
    public static final String ACTION_NOTIFICATION_DELETE = "notification_delete";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public NotificationWorker(String name) {
        super(name);
    }

    public NotificationWorker() {
        super("NotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        if (action == null) return;

        Message message = null;
        try {
            message = Message.parseIntent(intent);
        } catch (Exception ignore) {
        }

        if (message == null) return;

        switch (action) {
            case ACTION_CAROUSEL_IMG_CHANGE:
                int targetIndex = intent.getIntExtra(RichPushConstants.EXTRA_CAROUSEL_INDEX, 0);
                int notificationId = intent.getIntExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, 0);

                updateCarouselNotification(this, message, targetIndex, notificationId);

                break;

            case ACTION_NOTIFICATION_DELETE:
                /**
                 * Remove if there is any cached images (used for carousel) found for this notification.
                 */
                NotificationUtils.removeCachedCarouselImages(this, message);

                break;
        }
    }

    private void updateCarouselNotification(Context context, Message message, int newIndex, int notificationId) {
        CustomNotificationFactory
                .getInstance()
                .createAndShowCarousel(context, message, true, newIndex, notificationId);
    }
}
