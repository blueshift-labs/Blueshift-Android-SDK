package com.blueshift.rich_push;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftImageCache;
import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.model.Configuration;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.NotificationUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class is responsible for building custom notifications supported by the sdk.
 * Ex: Carousel Notifications.
 * <p>
 * Created by Rahul on 16/9/16.
 */
class CustomNotificationFactory {

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
    void createAndShowAnimatedCarousel(Context context, Message message) {
        if (context != null && message != null) {
            int notificationId = NotificationFactory.getRandomNotificationId();

            NotificationCompat.Builder builder = createBasicNotification(context, message, false, notificationId);
            if (builder != null) {
                Notification notification = builder.build();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    notification.bigContentView = createAnimatedCarousal(context, message, notificationId);
                }

                builder.setDeleteIntent(getNotificationDeleteIntent(context, message));

                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel =
                                NotificationUtils.createNotificationChannel(context, message);
                        if (channel != null) {
                            manager.createNotificationChannel(channel);
                        }
                    }

                    try {
                        manager.notify(notificationId, notification);

                        // Acknowledge that we received and displayed the push message.
                        NotificationUtils.invokePushDelivered(context, message);
                    } catch (Exception e) {
                        BlueshiftLogger.e(LOG_TAG, e);
                    }
                }
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
    void createAndShowCarousel(Context context, Message message) {
        int notificationId = NotificationFactory.getRandomNotificationId();

        if (message != null) {
            CarouselElement[] elements = message.getCarouselElements();
            if (elements != null) {
                for (CarouselElement element : elements) {
                    // preload scaled bitmaps
                    BlueshiftImageCache.preloadScaled(
                            context,
                            element.getImageUrl(),
                            RichPushConstants.BIG_IMAGE_WIDTH,
                            RichPushConstants.BIG_IMAGE_HEIGHT
                    );
                }
            }
        }

        createAndShowCarousel(context, message, false, 0, notificationId);
    }

    /**
     * This method creates and displays a carousal notification with an image at the given index.
     *
     * @param context     valid {@link Context} object
     * @param message     valid {@link Message} object with carousel elements
     * @param isUpdating  flag to indicate id the notification should be created or updated
     * @param targetIndex index of the image to be shown in carousel - carousel element index
     */
    void createAndShowCarousel(Context context, Message message, boolean isUpdating, int targetIndex, int notificationId) {
        if (context != null && message != null) {
            NotificationCompat.Builder builder = createBasicNotification(context, message, isUpdating, notificationId);
            if (builder != null) {
                Notification notification = builder.build();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    notification.bigContentView = getCarouselImage(context, message, isUpdating, targetIndex, notificationId);
                }

                builder.setDeleteIntent(getNotificationDeleteIntent(context, message));

                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel =
                                NotificationUtils.createNotificationChannel(context, message);
                        if (channel != null) {
                            manager.createNotificationChannel(channel);
                        }
                    }

                    try {
                        manager.notify(notificationId, notification);

                        // Acknowledge that we received and displayed the push message.
                        if (!isUpdating) NotificationUtils.invokePushDelivered(context, message);
                    } catch (Exception e) {
                        BlueshiftLogger.e(LOG_TAG, e);
                    }
                }
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
    private RemoteViews getCarouselImage(Context context, Message message, boolean isUpdating, int currentIndex, int notificationId) {
        RemoteViews contentView = null;

        if (context != null) {
            contentView = new RemoteViews(context.getPackageName(), R.layout.bsft_carousel_layout);

            setBasicNotificationData(context, message, contentView, true);

            if (message != null && message.getCarouselLength() > 0) {
                contentView.setViewVisibility(R.id.big_picture, View.VISIBLE);

                applyAndroid12UiChanges(contentView, context);

                // show next and prev buttons only when we have more than 1 carousel element
                if (message.getCarouselLength() > 1) {
                    contentView.setViewVisibility(R.id.next_button, View.VISIBLE);
                    contentView.setViewVisibility(R.id.prev_button, View.VISIBLE);
                }

                CarouselElement[] elements = message.getCarouselElements();
                CarouselElement element;
                if (currentIndex < elements.length && (element = elements[currentIndex]) != null) {
                    Bitmap bitmap = BlueshiftImageCache.getScaledBitmap(
                            context,
                            element.getImageUrl(),
                            RichPushConstants.BIG_IMAGE_WIDTH,
                            RichPushConstants.BIG_IMAGE_HEIGHT
                    );

                    if (bitmap != null) {
                        contentView.setImageViewBitmap(R.id.big_picture, bitmap);
                    } else {
                        // show the app icon as place holder for corrupted image
                        Configuration configuration = Blueshift.getInstance(context).getConfiguration();
                        if (configuration != null) {
                            contentView.setImageViewResource(R.id.big_picture, configuration.getAppIcon());
                        }
                    }

                    // remove any view found on the overlay container
                    contentView.removeAllViews(R.id.carousel_overlay_container);

                    // look for overlay content and add it in the container layout (if found)
                    RemoteViews overlay = getOverlayView(context, element);
                    if (overlay != null) {
                        contentView.addView(R.id.carousel_overlay_container, overlay);
                    }

                    contentView.setOnClickPendingIntent(
                            R.id.big_picture,
                            getCarouselImageClickPendingIntent(context, message, element, notificationId)
                    );

                    contentView.setOnClickPendingIntent(
                            R.id.next_button,
                            getNavigationPendingIntent(context, message, message.getNextCarouselIndex(currentIndex), notificationId)
                    );

                    contentView.setOnClickPendingIntent(
                            R.id.prev_button,
                            getNavigationPendingIntent(context, message, message.getPrevCarouselIndex(currentIndex), notificationId)
                    );
                }
            }
        }

        return contentView;
    }

    /**
     * This method is responsible for making the notification look good in Android 12.
     *
     * @param contentView the root content view
     * @param context     context object
     */
    private void applyAndroid12UiChanges(RemoteViews contentView, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // round corners for the expanded view for Android 12 and higher
            contentView.setInt(R.id.bsft_expanded_notification_s, "setBackgroundResource", R.drawable.bsft_rounded_corner_background);
            contentView.setBoolean(R.id.bsft_expanded_notification_s, "setClipToOutline", true);

            // apply margin for the apps that are not targeting android 12
            ApplicationInfo info = context.getApplicationInfo();
            if (info.targetSdkVersion < Build.VERSION_CODES.S) {
                int dp16 = CommonUtils.dpToPx(16, context);
                contentView.setViewPadding(R.id.bsft_custom_notification_container, dp16, dp16, dp16, dp16);
            }
        }
    }

    private RemoteViews getOverlayView(Context context, CarouselElement element) {
        RemoteViews overlayView = null;

        if (context != null && element != null) {
            CarouselElementText text = element.getContentText();
            CarouselElementText subText = element.getContentSubtext();

            if (text == null && subText == null) {
                return null;
            }

            // decide which layout and add it in the content view
            overlayView = new RemoteViews(context.getPackageName(), R.layout.bsft_carousel_text_overlay_view);

            String position = element.getContentLayoutType();

            // fill the overlay texts
            setOverlayText(overlayView, R.id.carousel_content_text, R.id.carousel_content_text_layout, text, position);
            setOverlayText(overlayView, R.id.carousel_content_subtext, R.id.carousel_content_subtext_layout, subText, position);

            // set gravity
            overlayView.setInt(R.id.carousel_content_layout, "setGravity", getElementGravity(position));
        }

        return overlayView;
    }

    private void setOverlayText(RemoteViews containerView, int textViewId, int containerViewId, CarouselElementText content, String position) {
        if (containerView != null && content != null && !TextUtils.isEmpty(content.getText())) {
            // BlueshiftLogger.d(LOG_TAG, "Carousel text: " + content.getText());

            containerView.setTextViewText(textViewId, content.getText());

            // color
            if (!TextUtils.isEmpty(content.getTextColor())) {
                // BlueshiftLogger.d(LOG_TAG, "Carousel text color: " + content.getTextColor());

                int color = Color.parseColor(content.getTextColor());
                containerView.setTextColor(textViewId, color);
            }

            // bg color
            if (!TextUtils.isEmpty(content.getTextBackgroundColor())) {
                // BlueshiftLogger.d(LOG_TAG, "Carousel text background color: " + content.getTextBackgroundColor());

                int color = Color.parseColor(content.getTextBackgroundColor());
                containerView.setInt(containerViewId, "setBackgroundColor", color);
            }

            // text gravity
            containerView.setInt(containerViewId, "setGravity", getElementTextGravity(position));

            // size
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // BlueshiftLogger.d(LOG_TAG, "Carousel text size: " + content.getTextSize());

                if (content.getTextSize() > 0) {
                    containerView.setTextViewTextSize(
                            textViewId, TypedValue.COMPLEX_UNIT_SP, content.getTextSize());
                }
            }
        }
    }

    private int getElementGravity(String position) {
        int gravity = Gravity.CENTER;

        if (position != null) {
            String[] parts = position.split("_");
            if (parts != null && parts.length == 2) {
                switch (parts[0]) {
                    case "top":
                        gravity = Gravity.TOP;
                        break;

                    case "center":
                        gravity = Gravity.CENTER_VERTICAL;
                        break;

                    case "bottom":
                        gravity = Gravity.BOTTOM;
                        break;
                }
            }
        }

        return gravity;
    }

    private int getElementTextGravity(String position) {
        int gravity = Gravity.CENTER;

        if (position != null) {
            String[] parts = position.split("_");
            if (parts != null && parts.length == 2) {
                switch (parts[1]) {
                    case "left":
                        gravity = Gravity.START;
                        break;

                    case "center":
                        gravity = Gravity.CENTER;
                        break;

                    case "right":
                        gravity = Gravity.END;
                        break;
                }
            }
        }

        return gravity;
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

            // check if large icon is available. If yes, set it
            Bitmap largeIcon = BlueshiftImageCache.getScaledBitmap(
                    context,
                    message.getLargeIconUrl(),
                    RichPushConstants.BIG_IMAGE_WIDTH,
                    RichPushConstants.BIG_IMAGE_HEIGHT);

            if (largeIcon != null) {
                if (isExpanded) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        contentView.setViewVisibility(R.id.icon_group, View.GONE);
                    } else {
                        // The icon comes on the left side for API levels less than 24.
                        // So, the icon should stay when expanded.
                        contentView.setImageViewBitmap(R.id.notification_icon, largeIcon);
                    }
                } else {
                    contentView.setImageViewBitmap(R.id.notification_icon, largeIcon);
                }

                int smallIconResId = configuration.getSmallIconResId();
                if (smallIconResId != 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                        // show small icon
                        contentView.setViewVisibility(R.id.notification_small_icon, View.VISIBLE);
                        contentView.setImageViewResource(R.id.notification_small_icon, smallIconResId);

                        // app name
                        CharSequence appName = CommonUtils.getAppName(context);
                        contentView.setTextViewText(R.id.notification_app_name_text, appName);

                        // set icon color
                        int bgColor = configuration.getNotificationColor();
                        if (bgColor != 0) {
                            bgColor |= 0xFF000000; // no alpha for custom colors (AOSP)
                            contentView.setInt(R.id.notification_small_icon, "setColorFilter", bgColor);

                            // after version OREO, text color isn't changing.
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                                contentView.setInt(R.id.notification_app_name_text, "setTextColor", bgColor);
                            }
                        }
                    } else {
                        contentView.setViewVisibility(R.id.notification_small_icon, View.VISIBLE);
                        contentView.setViewVisibility(R.id.notification_small_icon_background, View.VISIBLE);

                        contentView.setImageViewResource(R.id.notification_small_icon, smallIconResId);

                        // set background color
                        int bgColor = configuration.getNotificationColor();
                        if (bgColor != 0) {
                            contentView.setInt(R.id.notification_small_icon_background, "setColorFilter", bgColor);
                        }
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // hide notification big icon
                    contentView.setViewVisibility(R.id.icon_group, View.GONE);

                    int smallIconResId = configuration.getSmallIconResId();
                    if (smallIconResId != 0) {
                        // show small icon
                        contentView.setViewVisibility(R.id.notification_small_icon, View.VISIBLE);
                        contentView.setImageViewResource(R.id.notification_small_icon, smallIconResId);

                        // app name
                        CharSequence appName = CommonUtils.getAppName(context);
                        contentView.setTextViewText(R.id.notification_app_name_text, appName);

                        // set icon color
                        int bgColor = configuration.getNotificationColor();
                        if (bgColor != 0) {
                            bgColor |= 0xFF000000; // no alpha for custom colors (AOSP)
                            contentView.setInt(R.id.notification_small_icon, "setColorFilter", bgColor);

                            // after version OREO, text color isn't changing.
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                                contentView.setInt(R.id.notification_app_name_text, "setTextColor", bgColor);
                            }
                        }
                    }
                } else {
                    // set notification color
                    int bgColor = configuration.getNotificationColor();
                    if (bgColor == 0) {
                        bgColor = ContextCompat.getColor(context, android.R.color.darker_gray);
                    }
                    contentView.setInt(R.id.notification_icon, "setBackgroundColor", bgColor);

                    // make overlay visible
                    contentView.setViewVisibility(R.id.notification_icon_overlay, View.VISIBLE);

                    // set small icon on the center
                    int smallIconResId = configuration.getSmallIconResId();
                    if (smallIconResId != 0) {
                        contentView.setImageViewResource(R.id.notification_icon, smallIconResId);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            // align the icon to center
                            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                            int dp11 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 11.0f, metrics);
                            contentView.setViewPadding(R.id.notification_icon, dp11, dp11, dp11, dp11);

                            // set tint color
                            int white = ContextCompat.getColor(context, android.R.color.white);
                            contentView.setInt(R.id.notification_icon, "setColorFilter", white);
                        }
                    }
                }
            }

            contentView.setTextViewText(R.id.notification_content_text, message.getContentText());

            float textSize = 12f;
            // int textAppearance = R.style.NotificationLine2;

            if (isExpanded) {
                contentView.setTextViewText(R.id.notification_content_title, message.getBigContentTitle());

                String bigContentSummary = message.getBigContentSummaryText();
                if (!TextUtils.isEmpty(bigContentSummary)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        String dot = context.getString(R.string.bsft_notification_dot);
                        bigContentSummary = " " + dot + " " + bigContentSummary;
                    } else {
                        // we have both content and content sub text. make room for content sub text
                        // make content text single line. applicable to pre Nougat versions.

                        contentView.setInt(R.id.notification_content_text, "setLines", 1);
                    }

                    contentView.setViewVisibility(R.id.notification_sub_text, View.VISIBLE);
                    contentView.setTextViewText(R.id.notification_sub_text, bigContentSummary);
                } else {
                    contentView.setViewVisibility(R.id.notification_sub_text, View.GONE);

                    textSize = 14f;
                    // textAppearance = R.style.Notification;
                }
            } else {
                contentView.setTextViewText(R.id.notification_content_title, message.getContentTitle());

                String contentSubText = message.getContentSubText();
                if (!TextUtils.isEmpty(contentSubText)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        String dot = context.getString(R.string.bsft_notification_dot);
                        contentSubText = " " + dot + " " + contentSubText;
                    } else {
                        // we have both content and content sub text. make room for content sub text
                        // make content text single line. applicable to pre Nougat versions.

                        contentView.setInt(R.id.notification_content_text, "setLines", 1);
                    }

                    contentView.setViewVisibility(R.id.notification_sub_text, View.VISIBLE);
                    contentView.setTextViewText(R.id.notification_sub_text, contentSubText);
                } else {
                    contentView.setViewVisibility(R.id.notification_sub_text, View.GONE);

                    textSize = 14f;
                    // textAppearance = R.style.Notification;
                }
            }

            /*
             * We can not call textAppearance(Context, int) on RemoteView's TextViews.
             * Hence using static text sizes. 12sp for smaller text and 14sp for bigger ones.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // bigger text size on N+ devices for content text
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    contentView.setTextViewTextSize(R.id.notification_content_text, TypedValue.COMPLEX_UNIT_SP, 14);
                } else {
                    contentView.setTextViewTextSize(R.id.notification_content_text, TypedValue.COMPLEX_UNIT_SP, textSize);
                }
            }

            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                contentView.setInt(R.id.notification_content_text, "setTextAppearance", textAppearance);
            } else {
                *//*
             * We can not call textAppearance(Context, int) on RemoteView's TextViews.
             * Hence using static text sizes. 12sp for smaller text and 14sp for bigger ones.
             *//*
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    contentView.setTextViewTextSize(R.id.notification_content_text, TypedValue.COMPLEX_UNIT_SP, textSize);
                }
            }
            */

            // workaround to hide the time on N+ devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                String notificationTime = new SimpleDateFormat(
                        "hh:mm aa", Locale.getDefault())
                        .format(new Date(System.currentTimeMillis()));

                // String dot = context.getString(R.string.dot);
                // contentView.setTextViewText(R.id.notification_time, " " + dot + " " + notificationTime);
                contentView.setTextViewText(R.id.notification_time, notificationTime);
            }
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
    private NotificationCompat.Builder createBasicNotification(Context context, Message message, boolean isUpdating, int notificationId) {
        NotificationCompat.Builder builder = null;

        if (context != null && message != null) {
            Configuration configuration = Blueshift.getInstance(context).getConfiguration();
            if (configuration != null) {
                String channelId = NotificationUtils.getNotificationChannelId(context, message);
                builder = new NotificationCompat.Builder(context, channelId);
                builder.setDefaults(Notification.DEFAULT_ALL);
                builder.setAutoCancel(true);
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
                builder.setOnlyAlertOnce(true);

                // set basic items
                builder.setSmallIcon(configuration.getSmallIconResId());
                builder.setContentTitle(message.getContentTitle());
                builder.setContentText(message.getContentText());

                RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.bsft_notification_basic_layout);
                builder.setContent(contentView);

                setBasicNotificationData(context, message, contentView, false);

                // set notification click action as 'app open' by default
                String action = RichPushConstants.ACTION_OPEN_APP(context);
                builder.setContentIntent(
                        NotificationFactory.getNotificationClickPendingIntent(action, context, message, notificationId));
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
    private RemoteViews createAnimatedCarousal(Context context, Message message, int notificationId) {
        RemoteViews contentView = null;

        if (context != null && message != null) {
            String packageName = context.getPackageName();
            if (!TextUtils.isEmpty(packageName)) {
                // create/inflate carousel notification's layout.
                contentView = new RemoteViews(packageName, R.layout.bsft_carousel_layout);

                // show the container to host ViewFlipper.
                contentView.setViewVisibility(R.id.animated_carousel_view, View.VISIBLE);

                applyAndroid12UiChanges(contentView, context);

                // load basic notification contents.
                setBasicNotificationData(context, message, contentView, true);

                // create/inflate ViewFlipper layout.
                RemoteViews viewFlipper = new RemoteViews(packageName, R.layout.bsft_carousel_view_flipper);

                // loop through the carousel elements and add the image into ViewFlipper.
                CarouselElement[] elements = message.getCarouselElements();
                if (elements != null) {
                    for (CarouselElement element : elements) {
                        if (element == null) continue;

                        Bitmap bitmap = BlueshiftImageCache.getScaledBitmap(
                                context,
                                element.getImageUrl(),
                                RichPushConstants.BIG_IMAGE_WIDTH,
                                RichPushConstants.BIG_IMAGE_HEIGHT
                        );

                        if (bitmap == null) continue;

                        // Set the image into the view.
                        RemoteViews viewFlipperEntry = new RemoteViews(packageName, R.layout.bsft_carousel_view_flipper_entry);
                        viewFlipperEntry.setImageViewBitmap(R.id.carousel_image_view, bitmap);

                        // Attach an onClick pending intent.
                        viewFlipperEntry.setOnClickPendingIntent(
                                R.id.carousel_image_view,
                                getCarouselImageClickPendingIntent(context, message, element, notificationId)
                        );

                        // remove any view found on the overlay container
                        viewFlipperEntry.removeAllViews(R.id.carousel_overlay_container);

                        // look for overlay content and add it in the container layout (if found)
                        RemoteViews overlay = getOverlayView(context, element);
                        if (overlay != null) {
                            viewFlipperEntry.addView(R.id.carousel_overlay_container, overlay);
                        }

                        // Add the image into ViewFlipper.
                        viewFlipper.addView(R.id.view_flipper, viewFlipperEntry);
                    }
                }

                // add view flipper to the content view
                contentView.addView(R.id.animated_carousel_view, viewFlipper);
            }
        }

        return contentView;
    }

    /**
     * Helper method to return pending intent required by the onClick
     * action on Next/Previous button on Carousel Notification.
     *
     * @param context        valid context
     * @param message        valid message
     * @param targetIndex    the index of next image to be displayed in carousel
     * @param notificationId id of the notification to be updated with next/prev image
     * @return {@link PendingIntent}
     */
    private PendingIntent getNavigationPendingIntent(Context context, Message message, int targetIndex, int notificationId) {
        Intent intent = new Intent(context, NotificationWorker.class);
        intent.setAction(NotificationWorker.ACTION_CAROUSEL_IMG_CHANGE);

        intent.putExtra(RichPushConstants.EXTRA_CAROUSEL_INDEX, targetIndex);
        intent.putExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationId);
        if (message != null) {
            intent.putExtra(RichPushConstants.EXTRA_MESSAGE, message.toJson());
        }

        int code = NotificationFactory.getRandomPIRequestCode();

        return PendingIntent.getService(context, code, intent, CommonUtils.appendImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    /**
     * Creates pending intent to attach with click actions on carousel images. Each image shown
     * in the carousel will have a separate click action. Default action will be app open.
     *
     * @param context        valid context object
     * @param message        valid message object
     * @param element        corresponding carousel element
     * @param notificationId id of the notification being clicked
     * @return {@link PendingIntent}
     */
    private PendingIntent getCarouselImageClickPendingIntent(Context context, Message message, CarouselElement element, int notificationId) {
        Bundle bundle = new Bundle();
        bundle.putInt(RichPushConstants.EXTRA_NOTIFICATION_ID, notificationId);

        if (message != null) bundle.putString(RichPushConstants.EXTRA_MESSAGE, message.toJson());

        if (element != null) {
            bundle.putString(RichPushConstants.EXTRA_CAROUSEL_ELEMENT, element.toJson());
            bundle.putString(RichPushConstants.EXTRA_DEEP_LINK_URL, element.getDeepLinkUrl());
        }

        // get the activity to handle clicks (user defined or sdk defined
        Intent intent = NotificationUtils.getNotificationEventsActivity(context, bundle);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addNextIntent(intent);

        int reqCode = NotificationFactory.getRandomPIRequestCode();
        return taskStackBuilder.getPendingIntent(reqCode, CommonUtils.appendImmutableFlag(PendingIntent.FLAG_ONE_SHOT));
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

        if (message != null) {
            delIntent.putExtra(RichPushConstants.EXTRA_MESSAGE, message.toJson());
        }

        int reqCode = NotificationFactory.getRandomPIRequestCode();
        return PendingIntent.getService(context, reqCode, delIntent, CommonUtils.appendImmutableFlag(PendingIntent.FLAG_ONE_SHOT));
    }
}
