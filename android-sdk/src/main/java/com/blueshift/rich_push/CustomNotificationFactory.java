package com.blueshift.rich_push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.blueshift.Blueshift;
import com.blueshift.R;
import com.blueshift.model.Configuration;
import com.blueshift.util.NotificationUtils;
import com.blueshift.util.SdkLog;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class is responsible for building custom notifications supported by the sdk.
 * Ex: Carousel Notifications.
 * <p>
 * Created by Rahul on 16/9/16.
 */
public class CustomNotificationFactory {

    private static final String LOG_TAG = "NotificationFactory";

    private static CustomNotificationFactory sInstance;

    private CustomNotificationFactory() {
        // do nothing for now.
    }

    public static CustomNotificationFactory getInstance() {
        if (sInstance == null) {
            sInstance = new CustomNotificationFactory();
        }

        return sInstance;
    }

    /**
     * Method to create and display notification with carousel with images displayed
     * one after another with fade in/out animation.
     *
     * @param context valid context object.
     * @param message message object with valid carousel elements.
     */
    public void createAndShowAnimatedCarousel(Context context, Message message) {
        if (context != null && message != null) {
            NotificationCompat.Builder builder = createBasicNotification(context, message, false);
            if (builder != null) {
                Notification notification = builder.build();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    notification.bigContentView = createAnimatedCarousal(context, message);
                }

                builder.setDeleteIntent(getNotificationDeleteIntent(context, message));

                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(message.getCategory().getNotificationId(), notification);
            }
        }
    }

    /**
     * Method to display notification with image carousel. This one will have 2 buttons.
     * Next and Previous buttons to navigate through images.
     *
     * @param context valid context object.
     * @param message message object with valid carousel elements.
     */
    public void createAndShowCarousel(Context context, Message message) {
        createAndShowCarousel(context, message, false, 0);
    }

    /**
     * This method creates and displays a carousal notification with an image at the given index.
     *
     * @param context     valid {@link Context} object
     * @param message     valid {@link Message} object with carousel elements
     * @param isUpdating  flag to indicate id the notification should be created or updated
     * @param targetIndex index of the image to be shown in carousel - carousel element index
     */
    public void createAndShowCarousel(Context context, Message message, boolean isUpdating, int targetIndex) {
        if (context != null && message != null) {
            NotificationCompat.Builder builder = createBasicNotification(context, message, isUpdating);
            if (builder != null) {
                Notification notification = builder.build();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    notification.bigContentView = getCarouselImage(context, message, isUpdating, targetIndex);
                }

                builder.setDeleteIntent(getNotificationDeleteIntent(context, message));

                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(message.getCategory().getNotificationId(), notification);
            }
        }
    }

    /**
     * Method that creates view required by the non-animated image carousel notification.
     *
     * @param context valid context object.
     * @param message message object with valid carousel elements.
     * @return RemoteView object with notification details filled in.
     */
    private RemoteViews getCarouselImage(Context context, Message message, boolean isUpdating, int currentIndex) {
        RemoteViews contentView = null;

        if (context != null) {
            contentView = new RemoteViews(context.getPackageName(), R.layout.carousel_layout);

            contentView.setViewVisibility(R.id.big_picture, View.VISIBLE);
            contentView.setViewVisibility(R.id.next_button, View.VISIBLE);
            contentView.setViewVisibility(R.id.prev_button, View.VISIBLE);

            setBasicNotificationData(context, message, contentView, true);

            if (message != null && message.getCarouselLength() > 0) {
                CarouselElement[] elements = message.getCarouselElements();
                if (currentIndex < elements.length) {
                    CarouselElement element = elements[currentIndex];

                    String imageFileName = NotificationUtils.getImageFileName(element.getImageUrl());
                    Bitmap bitmap = NotificationUtils.loadImageFromDisc(context, imageFileName);

                    // if the image is null and we are creating the notification for the first time,
                    // it means the cache is empty. so let's try downloading the images.
                    if (bitmap == null && !isUpdating) {
                        // image is unavailable. update the image cache.
                        NotificationUtils.downloadCarouselImages(context, message);

                        bitmap = NotificationUtils.loadImageFromDisc(context, imageFileName);
                        if (bitmap == null) {
                            SdkLog.e(LOG_TAG, "Could not load image for carousel.");
                        }
                    }

                    if (bitmap != null) {
                        contentView.setImageViewBitmap(R.id.big_picture, bitmap);
                    } else {
                        // show the app icon as place holder for corrupted image
                        Configuration configuration = Blueshift.getInstance(context).getConfiguration();
                        if (configuration != null) {
                            contentView.setImageViewResource(R.id.big_picture, configuration.getAppIcon());
                        }
                    }

                    contentView.setOnClickPendingIntent(
                            R.id.big_picture,
                            getCarouselImageClickPendingIntent(context, message, element)
                    );

                    contentView.setOnClickPendingIntent(
                            R.id.next_button,
                            getNavigationPendingIntent(context, message, message.getNextCarouselIndex(currentIndex))
                    );

                    contentView.setOnClickPendingIntent(
                            R.id.prev_button,
                            getNavigationPendingIntent(context, message, message.getPrevCarouselIndex(currentIndex))
                    );
                }
            }
        }

        return contentView;
    }

    /**
     * Method to fill basic notification details like title, content, icon etc.
     * This is required by both normal and expanded view creation methods of notification.
     *
     * @param context     valid context object
     * @param message     valid message object
     * @param contentView contentView on which the basic details should be filled in
     * @param isExpanded  flag to indicate if the basic notification is for expanded notification or not
     */
    private void setBasicNotificationData(Context context, Message message, RemoteViews contentView, boolean isExpanded) {
        if (context != null && message != null && contentView != null) {
            Configuration configuration = Blueshift.getInstance(context).getConfiguration();

            String notificationTime = new SimpleDateFormat("hh:mm aa", Locale.getDefault())
                    .format(new Date(System.currentTimeMillis()));

            contentView.setImageViewResource(R.id.notification_icon, configuration.getLargeIconResId());
            contentView.setImageViewResource(R.id.notification_small_icon, configuration.getSmallIconResId());

            contentView.setTextViewText(R.id.notification_content_text, message.getContentText());

            if (isExpanded) {
                contentView.setTextViewText(R.id.notification_content_title, message.getBigContentTitle());
                contentView.setTextViewText(R.id.notification_sub_text, message.getBigContentSummaryText());
            } else {
                contentView.setTextViewText(R.id.notification_content_title, message.getContentTitle());
                contentView.setTextViewText(R.id.notification_sub_text, message.getContentSubText());
            }

            contentView.setTextViewText(R.id.notification_time, notificationTime);
        }
    }

    /**
     * Created the basic notification (unexpanded view) and returns
     * the builder object with the generated view attached to it as
     * content view. The onClick action is to open the app and pass
     * the message object in it.
     *
     * @param context valid context object
     * @param message valid message object
     * @return {@link NotificationCompat.Builder} object with basic values filled in
     */
    private NotificationCompat.Builder createBasicNotification(Context context, Message message, boolean isUpdating) {
        NotificationCompat.Builder builder = null;

        if (context != null && message != null) {
            Configuration configuration = Blueshift.getInstance(context).getConfiguration();
            if (configuration != null) {
                builder = new NotificationCompat.Builder(context);
                builder.setDefaults(Notification.DEFAULT_ALL);
                builder.setAutoCancel(true);

                if (isUpdating) {
                    builder.setDefaults(0);
                }

                // set basic items
                builder.setSmallIcon(configuration.getAppIcon());
                builder.setContentTitle(message.getContentTitle());
                builder.setContentText(message.getContentText());

                RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_basic_layout);
                builder.setContent(contentView);

                setBasicNotificationData(context, message, contentView, false);

                // set notification click action as 'app open' by default
                String action = RichPushConstants.ACTION_OPEN_APP(context);
                builder.setContentIntent(getNotificationClickPendingIntent(action, context, message));
            }
        }

        return builder;
    }

    /**
     * This method is responsible for inflating the animated carousel view.
     * This method will download the images and will add actions to it.
     * The developers can add actions in payload JSON inside carousel element array item.
     * If no actions given, then app will be opened and 'message' object will be passed in.
     *
     * @param context valid {@link Context} object
     * @param message valid {@link Message} object
     * @return {@link RemoteViews} object with carousel set ready
     */
    private RemoteViews createAnimatedCarousal(Context context, Message message) {
        RemoteViews contentView = null;

        if (context != null && message != null) {
            contentView = new RemoteViews(context.getPackageName(), R.layout.carousel_layout);

            contentView.setViewVisibility(R.id.animated_carousel_view, View.VISIBLE);

            setBasicNotificationData(context, message, contentView, true);

            int index = 0;

            CarouselElement[] elements = message.getCarouselElements();
            for (CarouselElement element : elements) {
                try {
                    int resId = getImageViewResId(index++);
                    if (resId != -1) {
                        URL imageURL = new URL(element.getImageUrl());
                        Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());

                        contentView.setImageViewBitmap(resId, bitmap);

                        contentView.setOnClickPendingIntent(
                                resId,
                                getCarouselImageClickPendingIntent(context, message, element)
                        );
                    }
                } catch (IOException e) {
                    SdkLog.e(LOG_TAG, "Could not download image. " + e.getMessage());
                }
            }
        }

        return contentView;
    }

    /**
     * Helper method to tell the view id for carousel image (based on index)
     *
     * @param index valid index (0-3).
     * @return valid resource id of {@link android.widget.ImageView} in carousel layout.
     */
    private int getImageViewResId(int index) {
        switch (index) {
            case 0:
                return R.id.carousel_picture1;

            case 1:
                return R.id.carousel_picture2;

            case 2:
                return R.id.carousel_picture3;

            case 3:
                return R.id.carousel_picture4;
        }

        return -1;
    }

    /**
     * Helper method to return pending intent required by the onClick
     * action on Next/Previous button on Carousel Notification.
     *
     * @param context     valid context
     * @param message     valid message
     * @param targetIndex the index of next image to be displayed in carousel
     * @return {@link PendingIntent}
     */
    private PendingIntent getNavigationPendingIntent(Context context, Message message, int targetIndex) {
        Intent intent = new Intent(context, NotificationWorker.class);
        intent.setAction(NotificationWorker.ACTION_CAROUSEL_IMG_CHANGE);

        intent.putExtra(RichPushConstants.EXTRA_CAROUSEL_INDEX, targetIndex);
        intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);

        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Creates pending intent to attach with click actions on carousel images. Each image shown
     * in the carousel will have a separate click action. Default action will be app open.
     *
     * @param context valid context object
     * @param message valid message object
     * @param element corresponding carousel element
     * @return {@link PendingIntent}
     */
    private PendingIntent getCarouselImageClickPendingIntent(Context context, Message message, CarouselElement element) {
        String action = RichPushConstants.buildAction(context, element.getAction());
        Intent bcIntent = new Intent(action);

        bcIntent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, message.getCategory().getNotificationId());
        bcIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);
        bcIntent.putExtra(RichPushConstants.EXTRA_CAROUSEL_ELEMENT, element);

        return PendingIntent.getBroadcast(context, 0, bcIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    /**
     * Helper method to return valid pending intent to notify the clicks on notifications.
     * This uses the same actions we have given for normal notification clicks.
     *
     * @param action  actions that {@link RichPushActionReceiver} supports
     * @param context valid context object
     * @param message valid message object
     * @return {@link PendingIntent}
     */
    private PendingIntent getNotificationClickPendingIntent(String action, Context context, Message message) {
        Intent bcIntent = new Intent(action);

        bcIntent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, message.getCategory().getNotificationId());
        bcIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);

        return PendingIntent.getBroadcast(context, 0, bcIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    /**
     * This method generated the pending intent to be called when notification is deleted.
     *
     * @param context valid context object
     * @param message valid message object
     * @return {@link PendingIntent}
     */
    private PendingIntent getNotificationDeleteIntent(Context context, Message message) {
        Intent delIntent = new Intent(context, NotificationWorker.class);
        delIntent.setAction(NotificationWorker.ACTION_NOTIFICATION_DELETE);

        delIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message);

        return PendingIntent.getService(context, 0, delIntent, PendingIntent.FLAG_ONE_SHOT);
    }
}
