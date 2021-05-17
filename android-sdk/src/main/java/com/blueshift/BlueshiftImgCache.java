package com.blueshift;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.LruCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BlueshiftImgCache {
    private static final String TAG = "BlueshiftImgCache";

    private static final Object diskCacheLock = new Object();
    private static final int maxMemAvailable = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private static final int cacheSize = maxMemAvailable / 10;
    private static final LruCache<String, Bitmap> memCache = new LruCache<String, Bitmap>(cacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private static String FILE_NAME(String key) {
        return key != null ? "bsft_" + key : null;
    }

    public static boolean clean(Context context, String url) {
        String key = md5(url);
        return removeFromMemCache(key) || removeFromDiskCache(context, key);
    }

    public static Bitmap getBitmap(Context context, String url) {
        String key = md5(url);
        Bitmap bitmap = null;

        if (key != null) {
            BlueshiftLogger.d(TAG, "Attempting to load image.\t key:" + key + "\t url:" + url);

            bitmap = getFromMemCache(key);
            if (bitmap == null) {
                bitmap = getFromDiskCache(context, key);
                if (bitmap == null) {
                    bitmap = downloadScaledBitmap(url, 100, 100);
                    if (bitmap != null) {
                        addToMemCache(key, bitmap);
                        addToDiskCache(context, key, bitmap);
                    } else {
                        BlueshiftLogger.d(TAG, "Loading image from network");
                    }
                } else {
                    addToMemCache(key, bitmap);
                    BlueshiftLogger.d(TAG, "Loading image from disk");
                }
            } else {
                BlueshiftLogger.d(TAG, "Loading image from RAM");
            }
        }

        return bitmap;
    }

    public static void preload(Context context, String url) {
        Bitmap bitmap = getBitmap(context, url);
        if (bitmap != null) {
            BlueshiftLogger.d(TAG, "preload success " + url);
        }
    }

    private static String md5(String s) {
        if (s == null) return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static void addToDiskCache(Context context, String key, Bitmap val) {
        synchronized (diskCacheLock) {
            if (context != null) {
                FileOutputStream outputStream = null;

                try {
                    if (key != null && val != null) {
                        outputStream = context.openFileOutput(FILE_NAME(key), Context.MODE_PRIVATE);
                        val.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();
                    }
                } catch (FileNotFoundException ignored) {
                } catch (IOException ignored) {
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }

    private static Bitmap getFromDiskCache(Context context, String key) {
        synchronized (diskCacheLock) {
            Bitmap bitmap = null;

            if (context != null && key != null) {
                String filename = FILE_NAME(key);
                File cachedFile = context.getFileStreamPath(filename);
                if (cachedFile.exists()) {
                    InputStream inputStream = null;
                    try {
                        inputStream = context.openFileInput(filename);
                        bitmap = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    } catch (FileNotFoundException ignored) {
                    } catch (IOException ignored) {
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            }

            return bitmap;
        }
    }

    private static boolean removeFromDiskCache(Context context, String key) {
        if (context != null && key != null) {
            String filename = FILE_NAME(key);
            File cachedFile = context.getFileStreamPath(filename);
            if (cachedFile.exists()) {
                return cachedFile.delete();
            }
        }

        return false;
    }

    private static void addToMemCache(String key, Bitmap val) {
        if (getFromMemCache(key) == null) {
            memCache.put(key, val);
        }
    }

    private static Bitmap getFromMemCache(String key) {
        return memCache.get(key);
    }

    private static boolean removeFromMemCache(String key) {
        if (getFromMemCache(key) != null) {
            return memCache.remove(key) != null;
        }

        return false;
    }

    private static Bitmap downloadScaledBitmap(String url, int reqWidth, int reqHeight) {
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new URL(url).openStream(), new Rect(), options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            options.inJustDecodeBounds = false;

            Bitmap raw = BitmapFactory.decodeStream(new URL(url).openStream(), new Rect(), options);
            if (raw != null) {
                BlueshiftLogger.d(TAG, "Bitmap (" +
                        "size: " + (raw.getByteCount() / 1024f) / 1024f + " MB\t" +
                        "url: " + url + ")");
                return raw;
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
}
