package com.blueshift.rich_push;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by rahul on 20/9/16 @ 12:00 PM.
 */
public class NotificationWorker extends IntentService {

    public static final String ACTION_CAROUSEL_IMG_CHANGE = "carousel_image_change";

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

        Message message = (Message) intent.getSerializableExtra(RichPushConstants.EXTRA_MESSAGE);

        switch (action) {
            case ACTION_CAROUSEL_IMG_CHANGE:
                int targetIndex = intent.getIntExtra(RichPushConstants.EXTRA_CAROUSEL_INDEX, 0);
                message.setCarouselCurrentIndex(targetIndex);
                message.setUpdateNotification(true);

                updateCarouselNotification(message);

                break;

        }
    }

    private void updateCarouselNotification(Message message) {
        CustomNotificationFactory.getInstance().createAndShowCarousel(this, message);
    }
}
