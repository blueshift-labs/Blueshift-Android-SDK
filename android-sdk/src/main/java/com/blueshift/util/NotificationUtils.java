package com.blueshift.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.blueshift.rich_push.CarouselElement;
import com.blueshift.rich_push.Message;

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
     * @param notificationId notification id for generating unique channel id
     * @return Valid channel id for notification.
     */
    public static String getNotificationChannelId(int notificationId) {
        return "bsft_channel_" + notificationId;
    }

    /**
     * Creates a channel object with default details. This is required for Oreo to show Notification.
     *
     * @param context        valid context object
     * @param notificationId valid notification id to generate channel id
     * @return valid NotificationChannel object
     */
    public static NotificationChannel createNotificationChannel(Context context, int notificationId) {
        NotificationChannel channel = null;

        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        if (packageManager != null) {
            ApplicationInfo info;
            try {
                info = packageManager.getApplicationInfo(context.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                info = null;
            }

            // setting the channel name as app name for now.
            CharSequence name = (info != null) ? packageManager.getApplicationLabel(info) : "Unknown";

            // create channel
            String channelId = getNotificationChannelId(notificationId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT);

                // todo: add channel setting here if needed.
                // keeping everything as default for now.
            }
        }

        return channel;
    }
}
