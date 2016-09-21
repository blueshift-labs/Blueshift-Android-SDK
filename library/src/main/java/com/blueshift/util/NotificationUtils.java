package com.blueshift.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
 * Created by rahul on 20/9/16 @ 3:55 PM.
 */
public class NotificationUtils {
    public static String getImageFileName(String url) {
        if (url == null) return null;

        return url.substring(url.lastIndexOf('/') + 1);
    }

    public static void downloadCarouselImages(Context context, Message message) {
        if (context != null && message != null) {
            CarouselElement[] carouselElements = message.getCarouselElements();
            if (carouselElements != null) {
                for (CarouselElement element : carouselElements) {
                    FileOutputStream fileOutputStream = null;
                    try {
                        // download image
                        URL imageURL = new URL(element.getImageUrl());
                        Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());

                        // resize image
                        bitmap = resizeImageForDevice(context, bitmap);

                        // save image
                        String imageUrl = element.getImageUrl();
                        String fileName = getImageFileName(imageUrl);

                        if (!TextUtils.isEmpty(fileName)) {
                            fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
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

        return resizedBitmap;
    }

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

    public static void removeImageFromDisc(Context context, String fileName) {
        if (context != null && !TextUtils.isEmpty(fileName)) {
            context.deleteFile(fileName);
        }
    }

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
}
