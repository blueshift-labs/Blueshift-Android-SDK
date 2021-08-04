package com.blueshift.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.TaskStackBuilder;

import android.text.TextUtils;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftImageCache;
import com.blueshift.BlueshiftLogger;
import com.blueshift.model.Configuration;
import com.blueshift.pn.BlueshiftNotificationEventsActivity;
import com.blueshift.rich_push.CarouselElement;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.RichPushConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class with helper methods to show custom notification.
 * <p>
 * Created by Rahul on 20/9/16 @ 3:55 PM.
 */
public class NotificationUtils {

    private static final String LOG_TAG = "NotificationUtils";

    /**
     * Extracts the file name from the image file url
     *
     * @param url a valid image url
     * @return the file name of the image the url referring to
     */
    public static String getImageFileName(String url) {
        if (url == null) return null;

        return url.substring(url.lastIndexOf('/') + 1);
    }

    /**
     * Method to remove all carousel images cached.
     *
     * @param context valid context object
     * @param message message with valid carousel elements
     */
    public static void removeCachedCarouselImages(Context context, Message message) {
        if (context != null && message != null) {
            CarouselElement[] carouselElements = message.getCarouselElements();
            if (carouselElements != null && carouselElements.length > 0) {
                for (CarouselElement element : carouselElements) {
                    if (element != null) {
                        BlueshiftImageCache.clean(context, element.getImageUrl());
                    }
                }
            }
        }
    }

    /**
     * Generates channel id unique for this package
     *
     * @param channelName channel name for generating unique channel id
     * @return Valid channel id for notification.
     */
    @Deprecated
    public static String getNotificationChannelId(@NonNull String channelName) {
        return "bsft_channel_" + channelName;
    }

    /**
     * Read the channel id provided in message or config. If not found in both the places,
     * return "bsft_channel_General" as default channel id.
     *
     * @param context valid Context object
     * @param message message object to read the channel name from
     * @return valid notification channel id
     */
    public static String getNotificationChannelId(Context context, Message message) {
        String channelId = RichPushConstants.DEFAULT_CHANNEL_ID;

        if (message != null && !TextUtils.isEmpty(message.getNotificationChannelId())) {
            channelId = message.getNotificationChannelId();
        } else {
            if (context != null) {
                Blueshift blueshift = Blueshift.getInstance(context);
                Configuration config = blueshift.getConfiguration();
                if (config != null) {
                    String configuredChannelId = config.getDefaultNotificationChannelId();
                    if (!TextUtils.isEmpty(configuredChannelId)) {
                        channelId = configuredChannelId;
                    }
                }
            }
        }

        return channelId;
    }

    /**
     * Read default channel name from config if provided. If not return "General" as
     * channel name.
     *
     * @param context valid Context object
     * @param message message object to read the channel name from
     * @return valid notification channel name
     */
    public static String getNotificationChannelName(Context context, Message message) {
        String channelName = RichPushConstants.DEFAULT_CHANNEL_NAME;

        if (message != null && !TextUtils.isEmpty(message.getNotificationChannelName())) {
            channelName = message.getNotificationChannelName();
        } else {
            if (context != null) {
                Blueshift blueshift = Blueshift.getInstance(context);
                Configuration config = blueshift.getConfiguration();
                if (config != null) {
                    String channelNameStr = config.getDefaultNotificationChannelName();
                    if (!TextUtils.isEmpty(channelNameStr)) {
                        channelName = channelNameStr;
                    }
                }
            }
        }

        return channelName;
    }

    /**
     * Reads a channel description from the config object
     *
     * @param context valid context object
     * @param message message object to read the channel name from
     * @return valid channel description if provided, else null
     */
    public static String getNotificationChannelDescription(Context context, Message message) {
        String channelDescription = null;

        if (message != null && !TextUtils.isEmpty(message.getNotificationChannelDescription())) {
            channelDescription = message.getNotificationChannelDescription();
        } else {
            if (context != null) {
                Blueshift blueshift = Blueshift.getInstance(context);
                Configuration config = blueshift.getConfiguration();
                if (config != null) {
                    channelDescription = config.getDefaultNotificationChannelDescription();
                }
            }
        }

        return channelDescription;
    }

    /**
     * Creates a channel object with default details. This is required for Oreo to show Notification.
     *
     * @param message valid Message object
     * @return valid NotificationChannel object
     */
    public static NotificationChannel createNotificationChannel(Context context, Message message) {
        NotificationChannel channel = null;

        // read channel name & description from config/message
        String channelName = getNotificationChannelName(context, message);
        String channelDescription = getNotificationChannelDescription(context, message);

        // create channel id
        String channelId = getNotificationChannelId(context, message);
        BlueshiftLogger.d(LOG_TAG, "Notification Channel Id: " + channelId);

        // create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            BlueshiftLogger.d(LOG_TAG, "Notification Channel Name: " + channelName);

            channel = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);

            if (!TextUtils.isEmpty(channelDescription)) {
                BlueshiftLogger.d(LOG_TAG, "Notification Channel Description: " + channelDescription);

                channel.setDescription(channelDescription);
            }

            // todo: add more channel setting here if needed.
            // keeping everything as default for now.
        }

        BlueshiftLogger.d(LOG_TAG, "Notification Channel Creation - " + (channel != null ? "Done!" : "Failed!"));

        return channel;
    }

    /**
     * Checks for the activity responsible for handling notification clicks based on the action.
     *
     * @param context context object to create intent
     * @param action  action string
     * @param extras  extra params as bundle
     * @return Intent object to launch activity.
     */
    public static Intent getNotificationEventsActivity(Context context, String action, Bundle extras) {
        // check if user has his own implementation
        Intent intent = NotificationUtils.getUserDefinedNotificationEventsActivity(context);
        if (intent == null) {
            // if not use sdk's activity
            intent = new Intent(context, BlueshiftNotificationEventsActivity.class);
        }

        if (!TextUtils.isEmpty(action)) {
            intent.setAction(action);
        }

        if (extras != null) {
            intent.putExtras(extras);
        }

        return intent;
    }

    /**
     * Check if user has defined an activity to handle clicks. if yes, launch that first.
     * The check is made by searching for activities with intent filters that has the below action.
     * com.blueshift.NOTIFICATION_CLICK_EVENT
     *
     * @param context Application's context to get stack
     * @return Valid intent object to launch
     */
    private static Intent getUserDefinedNotificationEventsActivity(Context context) {
        Intent intent = null;

        if (context != null) {
            // search for service that handles notification clicks (custom or built-in)
            Intent activityIntent = new Intent();
            activityIntent.setAction("com.blueshift.NOTIFICATION_CLICK_EVENT");
            activityIntent.setPackage(context.getPackageName());

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> resInfo = packageManager.queryIntentActivities(activityIntent, 0);
            if (!resInfo.isEmpty()) {
                // read default service
                ActivityInfo activityInfo;

                // check if service is overridden
                if (resInfo.size() == 1) {
                    activityInfo = resInfo.get(0).activityInfo;
                } else {
                    BlueshiftLogger.d(LOG_TAG, "Declared more than one activity to receive this action.");

                    // consider adding backup activity info here if needed.
                    activityInfo = null;
                }

                if (activityInfo != null) {
                    ComponentName cmpActivity = new ComponentName(
                            activityInfo.applicationInfo.packageName, activityInfo.name);

                    intent = new Intent();
                    intent.setComponent(cmpActivity);
                }
            }
        }

        return intent;
    }

    /**
     * Get the activity marked as cart activity inside the configuration object with extras
     *
     * @param context Application's context to get configuration object
     * @param message Message object to get values required to add inside bundle
     * @return Valid intent object to launch
     */
    public static Intent getAddToCartActivityIntent(Context context, Message message) {
        Intent pageLauncherIntent = null;

        if (message != null && context != null) {
            Configuration configuration = BlueshiftUtils.getConfiguration(context);
            if (configuration != null && configuration.getCartPage() != null) {
                pageLauncherIntent = new Intent(context, configuration.getCartPage());
                // add product specific items.
                pageLauncherIntent.putExtra("product_id", message.getProductId());
                pageLauncherIntent.putExtra("mrp", message.getMrp());
                pageLauncherIntent.putExtra("price", message.getPrice());
                pageLauncherIntent.putExtra("data", message.getData());
            } else {
                BlueshiftLogger.i(LOG_TAG, "Could not find cart activity class inside configuration. Opening MAIN activity.");
            }
        }

        return pageLauncherIntent;
    }

    /**
     * Get the activity marked as cart activity inside the configuration object
     *
     * @param context Application's context to get configuration object
     * @param message Message object to get values required to add inside bundle
     * @return Valid intent object to launch
     */
    public static Intent getViewCartActivityIntent(Context context, Message message) {
        Intent pageLauncherIntent = null;

        if (message != null && context != null) {
            Configuration configuration = BlueshiftUtils.getConfiguration(context);
            if (configuration != null && configuration.getCartPage() != null) {
                pageLauncherIntent = new Intent(context, configuration.getCartPage());
            } else {
                BlueshiftLogger.i(LOG_TAG, "Could not find cart activity class inside configuration. Opening MAIN activity.");
            }
        }

        return pageLauncherIntent;
    }

    /**
     * Get the activity marked as product details activity inside the configuration object with extras
     *
     * @param context Application's context to get configuration object
     * @param message Message object to get values required to add inside bundle
     * @return Valid intent object to launch
     */
    public static Intent getViewProductActivityIntent(Context context, Message message) {
        Intent pageLauncherIntent = null;

        if (message != null && context != null) {
            Configuration configuration = BlueshiftUtils.getConfiguration(context);
            if (configuration != null && configuration.getProductPage() != null) {
                pageLauncherIntent = new Intent(context, configuration.getProductPage());
                // add product specific items.
                pageLauncherIntent.putExtra("product_id", message.getProductId());
                pageLauncherIntent.putExtra("mrp", message.getMrp());
                pageLauncherIntent.putExtra("price", message.getPrice());
                pageLauncherIntent.putExtra("data", message.getData());
            } else {
                BlueshiftLogger.i(LOG_TAG, "Could not find product activity class inside configuration. Opening MAIN activity.");
            }
        }

        return pageLauncherIntent;
    }

    /**
     * Get the activity marked as offer display activity inside the configuration object
     *
     * @param context Application's context to get configuration object
     * @param message Message object to get values required to add inside bundle
     * @return Valid intent object to launch
     */
    public static Intent getViewOffersActivityIntent(Context context, Message message) {
        Intent pageLauncherIntent = null;

        if (message != null && context != null) {
            Configuration configuration = BlueshiftUtils.getConfiguration(context);
            if (configuration != null && configuration.getOfferDisplayPage() != null) {
                pageLauncherIntent = new Intent(context, configuration.getOfferDisplayPage());
            } else {
                BlueshiftLogger.i(LOG_TAG, "Could not find offer's page activity class inside configuration. Opening MAIN activity.");
            }
        }

        return pageLauncherIntent;
    }

    /**
     * Get the activity marked as LAUNCHER in the AndroidManifest.xml
     *
     * @param context Application's context to get configuration object
     * @param message Message object to get values required to add inside bundle
     * @return Valid intent object to launch
     */
    public static Intent getOpenAppIntent(Context context, Message message) {
        Intent launcherIntent = null;

        if (message != null && context != null) {
            PackageManager packageManager = context.getPackageManager();
            launcherIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        }

        return launcherIntent;
    }

    public static Bitmap loadScaledBitmap(String url, int reqWidth, int reqHeight) {
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new URL(url).openStream(), new Rect(), options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            options.inJustDecodeBounds = false;

            Bitmap raw = BitmapFactory.decodeStream(new URL(url).openStream(), new Rect(), options);
            if (raw != null) {
                BlueshiftLogger.d(LOG_TAG, "Bitmap (" +
                        "size: " + (raw.getByteCount() / 1024f) / 1024f + " MB\t" +
                        "url: " + url + ")");
                return raw;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * This is a helper method that can decide what needs to be done when someone clicks on push msg.
     * This method is being called by the sdk from the {@link BlueshiftNotificationEventsActivity}
     * The host app can make use of this method if they would like to override the clicks on push msg.
     *
     * @param context valid context object
     * @param action  action from the intent
     * @param bundle  bundle inside the intent
     * @return true: if the click was handled by the sdk, false: if the click was not handled by the sdk.
     */
    public static boolean processNotificationClick(Context context, String action, Bundle bundle) {
        if (context != null && action != null && bundle != null) {
            Message message = Message.fromBundle(bundle);
            if (message != null) {
                try {
                    String deepLink = bundle.getString(RichPushConstants.EXTRA_DEEP_LINK_URL);

                    // mark 'click'
                    NotificationUtils.invokePushClicked(context, message, deepLink);

                    Intent intent = null;

                    if (!TextUtils.isEmpty(action)) {
                        if (action.equals(RichPushConstants.ACTION_OPEN_APP(context))) {
                            intent = NotificationUtils.getOpenAppIntent(context, message);
                        } else if (action.equals(RichPushConstants.ACTION_VIEW(context))) {
                            intent = NotificationUtils.getViewProductActivityIntent(context, message);
                        } else if (action.equals(RichPushConstants.ACTION_BUY(context))) {
                            intent = NotificationUtils.getAddToCartActivityIntent(context, message);
                        } else if (action.equals(RichPushConstants.ACTION_OPEN_CART(context))) {
                            intent = NotificationUtils.getViewCartActivityIntent(context, message);
                        } else if (action.equals(RichPushConstants.ACTION_OPEN_OFFER_PAGE(context))) {
                            intent = NotificationUtils.getViewOffersActivityIntent(context, message);
                        }
                    }

                    if (intent == null) {
                        // make sure the app is opened even if no category deep-links are available
                        intent = NotificationUtils.getOpenAppIntent(context, message);
                    }

                    // add complete bundle to the intent.
                    intent.putExtras(bundle);

                    try {
                        // add deep link URL to the intent's data as well.
                        if (deepLink != null) intent.setData(Uri.parse(deepLink));
                    } catch (Exception e) {
                        BlueshiftLogger.e(LOG_TAG, e);
                    }

                    // Note: This will create a new task and launch the app with the corresponding
                    // activity. As per the docs, the dev should add parent activity to all the
                    // activities registered in the manifest in order to get the back stack working
                    // doc: https://developer.android.com/training/notify-user/navigation#DirectEntry
                    TaskStackBuilder.create(context).addNextIntentWithParentStack(intent).startActivities();

                    // mark 'app_open'
                    Blueshift.getInstance(context).trackNotificationPageOpen(message, false);

                    // remove cached images(if any) for this notification
                    NotificationUtils.removeCachedCarouselImages(context, message);

                    // remove notification from tray
                    NotificationManager notificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        int notificationID = intent.getIntExtra(RichPushConstants.EXTRA_NOTIFICATION_ID, 0);
                        notificationManager.cancel(notificationID);
                    }

                    context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

                    // click was handled by Blueshift SDK
                    return true;
                } catch (Exception e) {
                    BlueshiftLogger.e(LOG_TAG, e);
                }
            } else {
                BlueshiftLogger.d(LOG_TAG, "No message found inside bundle.");
            }
        } else {
            BlueshiftLogger.d(LOG_TAG, "processNotificationClick: Invalid arguments " +
                    "(context: " + context + ", action: " + action + ", bundle: " + bundle + ").");
        }

        // click was not handled by Blueshift SDK
        return false;
    }

    /**
     * Simplified version of processNotificationClick(context, action, bundle)
     *
     * @param context valid context object
     * @param intent  valid intent
     */
    public static boolean processNotificationClick(Context context, Intent intent) {
        if (context != null && intent != null) {
            return processNotificationClick(context, intent.getAction(), intent.getExtras());
        } else {
            BlueshiftLogger.d(LOG_TAG, "processNotificationClick: Invalid arguments " +
                    "(context: " + context + ", intent: " + intent + ").");
            return false;
        }
    }

    /**
     * This helper method allows the sdk to call the listener method (if provided) along with
     * internal tracking method for push delivered.
     *
     * @param context valid {@link Context} object
     * @param message valid {@link Message} object
     */
    public static void invokePushDelivered(Context context, Message message) {
        if (Blueshift.getBlueshiftPushListener() != null && message != null) {
            Map<String, Object> attr = message.toMap();
            if (Blueshift.getBlueshiftPushListener() != null) {
                Blueshift.getBlueshiftPushListener().onPushDelivered(attr);
            }
        }

        Blueshift.getInstance(context).trackNotificationView(message);
    }

    /**
     * This helper method allows the sdk to call the listener method (if provided) along with
     * internal tracking method for push click.
     *
     * @param context valid {@link Context} object
     * @param message valid {@link Message} object
     */
    public static void invokePushClicked(Context context, Message message, String clickUrl) {
        if (Blueshift.getBlueshiftPushListener() != null && message != null) {
            Map<String, Object> attr = message.toMap();
            if (attr != null && clickUrl != null) {
                attr.put(BlueshiftConstants.KEY_CLICK_URL, clickUrl);
            }

            if (Blueshift.getBlueshiftPushListener() != null) {
                Blueshift.getBlueshiftPushListener().onPushClicked(attr);
            }
        }

        HashMap<String, Object> clickAttr = new HashMap<>();
        if (clickUrl != null) clickAttr.put(BlueshiftConstants.KEY_CLICK_URL, clickUrl);
        Blueshift.getInstance(context).trackNotificationClick(message, clickAttr);
    }
}
