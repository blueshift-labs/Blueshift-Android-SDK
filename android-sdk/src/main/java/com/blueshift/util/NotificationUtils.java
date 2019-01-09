package com.blueshift.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.model.Configuration;
import com.blueshift.rich_push.CarouselElement;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.RichPushConstants;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
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
            try {
                InputStream inputStream = context.openFileInput(fileName);
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
    public static String getNotificationChannelId(@NotNull String channelName) {
        return "bsft_channel_" + channelName;
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
        String channelId = getNotificationChannelId(channelName);
        Log.d(LOG_TAG, "Notification Channel Id: " + channelId);

        // create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(LOG_TAG, "Notification Channel Name: " + channelName);

            channel = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);

            if (!TextUtils.isEmpty(channelDescription)) {
                Log.d(LOG_TAG, "Notification Channel Description: " + channelDescription);

                channel.setDescription(channelDescription);
            }

            // todo: add more channel setting here if needed.
            // keeping everything as default for now.
        }

        Log.d(LOG_TAG, "Notification Channel Creation - " + (channel != null ? "Done!" : "Failed!"));

        return channel;
    }
}
