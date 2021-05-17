package com.blueshift;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.LruCache;
import android.widget.ImageView;

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

    public static Bitmap getBitmap(Context context, String url) {
        return getScaledBitmap(context, url, 0, 0);
    }

    public static Bitmap getScaledBitmap(Context context, String url, int reqWidth, int reqHeight) {
        String key = md5(url);
        Bitmap bitmap = null;

        if (key != null) {
            BlueshiftLogger.d(TAG, "Attempting to load image with url:" + url);

            bitmap = getFromMemCache(key);
            if (bitmap == null) {
                bitmap = getFromDiskCache(context, key);
                if (bitmap == null) {
                    bitmap = downloadScaledBitmap(url, reqWidth, reqHeight);
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

    public static void loadBitmapOntoImageView(final Context context, final String url, final ImageView imageView) {
        if (imageView != null) {
            BlueshiftExecutor.getInstance().runOnWorkerThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            final Bitmap bitmap = getBitmap(context, url);
                            if (bitmap != null) {
                                imageView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        imageView.setImageBitmap(bitmap);
                                    }
                                });
                            }
                        }
                    }
            );
        }
    }

    public static void preload(Context context, String url) {
        Bitmap bitmap = getBitmap(context, url);
        String status = bitmap != null ? "success" : "failed";
        BlueshiftLogger.d(TAG, "Preload " + status + " for url: " + url);
    }

    public static void clean(Context context, String url) {
        String key = md5(url);
        if (key != null) {
            BlueshiftLogger.d(TAG, "Removing image from memory: " + removeFromMemCache(key));
            BlueshiftLogger.d(TAG, "Removing image from disk: " + removeFromDiskCache(context, key));
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

            if (reqWidth == 0) reqWidth = options.outWidth;
            if (reqHeight == 0) reqHeight = options.outHeight;

            BlueshiftLogger.d(TAG, "reqWidth: " + reqWidth + ", reqHeight: " + reqHeight);

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
