package com.blueshift.inappmessage;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.TextView;

import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
import com.blueshift.util.NetworkUtils;

import java.io.File;

public class InAppMessageIconFont {
    private static final Boolean _LOCK = false;
    private static final String TAG = "InAppMessageIconFont";
    private static final String FILE_NAME = "Font+Awesome+5+Free-Solid-900.otf";
    private static Typeface sFontAwesomeFont = null;
    private static InAppMessageIconFont sInstance = null;

    private InAppMessageIconFont(Context context) {
        try {
            if (context != null) {
                File fontFile = getFontFile(context);
                if (fontFile.exists()) {
                    sFontAwesomeFont = Typeface.createFromFile(fontFile);
                } else {
                    updateFont(context);
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    public static InAppMessageIconFont getInstance(Context context) {
        synchronized (_LOCK) {
            if (sInstance == null) {
                sInstance = new InAppMessageIconFont(context);
            }

            return sInstance;
        }
    }

    public void apply(TextView textView) {
        synchronized (_LOCK) {
            if (textView != null && sFontAwesomeFont != null) {
                textView.setTypeface(sFontAwesomeFont);
            }
        }
    }

    public void updateFont(final Context context) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(new Runnable() {
            @Override
            public void run() {
                synchronized (_LOCK) {
                    try {
                        // download
                        String source = "https://bsftassets.s3-us-west-2.amazonaws.com/inapp/" + FILE_NAME;
                        File fontFile = getFontFile(context);

                        if (!fontFile.exists()) {
                            Log.d(TAG, "Downloading font to " + fontFile.getAbsolutePath());
                            NetworkUtils.downloadFile(source, fontFile.getAbsolutePath());
                        }

                        // refresh variables
                        sFontAwesomeFont = Typeface.createFromFile(fontFile);
                    } catch (Exception e) {
                        BlueshiftLogger.e(TAG, e);
                    }
                }
            }
        });
    }

    private File getFontFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }
}
