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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.blueshift.Blueshift;
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
import java.util.List;

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
     * Downloads all carousel images and stores them inside app's private file location after compressing them.
     *
     * @param context valid context object
     * @param message message object with valid carousel elements
     */
    public static void downloadCarouselImages(Context context, Message message) {
        if (context != null && message != null) {
            CarouselElement[] carouselElements = message.getCarouselElements();
            if (carouselElements != null) {
                for (CarouselElement element : carouselElements) {
                    FileOutputStream fileOutputStream = null;
                    try {
                        if (element != null) {
                            // download image
                            URL imageURL = new URL(element.getImageUrl());
                            Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());

                            // resize image
                            bitmap = resizeImageForDevice(context, bitmap);

                            // save image
                            String imageUrl = element.getImageUrl();
                            String fileName = getImageFileName(imageUrl);

                            if (!TextUtils.isEmpty(fileName)) {
                                if (bitmap != null) {
                                    fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                                    fileOutputStream.close();
                                }
                            }
                        }
                    } catch (IOException e) {
                        BlueshiftLogger.e(LOG_TAG, e);
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                BlueshiftLogger.e(LOG_TAG, e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method re-sizes the bitmap to have aspect ration 2:1 based on the device's dimension.
     *
     * @param context      valid context object
     * @param sourceBitmap the image to resize
     * @return resized image
     */
    public static Bitmap resizeImageForDevice(Context context, Bitmap sourceBitmap) {
        Bitmap resizedBitmap = null;

        if (sourceBitmap != null) {
            if (sourceBitmap.getWidth() > sourceBitmap.getHeight()) {
                DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

                // ideal image aspect ratio for notification is 2:1
                int newWidth = displayMetrics.widthPixels;
                int newHeight = newWidth / 2;

                resizedBitmap = Bitmap.createScaledBitmap(sourceBitmap, newWidth, newHeight, true);
            }
        }

        if (resizedBitmap == null) {
            resizedBitmap = sourceBitmap;
        }

        return resizedBitmap;
    }

    /**
     * Loads the image stored inside app's private file location
     *
     * @param context  valid context object
     * @param fileName name of the image to be retrieved
     * @return bitmap image with given filename (if exists)
     */
    public static Bitmap loadImageFromDisc(Context context, String fileName) {
        Bitmap bitmap = null;

        File imageFile = context.getFileStreamPath(fileName);
        if (imageFile.exists()) {
            InputStream inputStream = null;
            try {
                inputStream = context.openFileInput(fileName);
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            } catch (FileNotFoundException e) {
                BlueshiftLogger.e(LOG_TAG, e);
            } catch (IOException e) {
                BlueshiftLogger.e(LOG_TAG, e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        BlueshiftLogger.e(LOG_TAG, e);
                    }
                }
            }
        }

        return bitmap;
    }

    /**
     * Remove file with given name from app's private files directory.
     *
     * @param context  valid context object
     * @param fileName the name of the file to be removed.
     */
    public static void removeImageFromDisc(Context context, String fileName) {
        if (context != null && !TextUtils.isEmpty(fileName)) {
            context.deleteFile(fileName);
        }
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
                    String fileName = getImageFileName(element.getImageUrl());
                    removeImageFromDisc(context, fileName);
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
}
