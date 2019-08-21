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
                        // todo: remove static URL
                        String source = "https://firebasestorage.googleapis.com/v0/b/cargonex-6251f.appspot.com/o/FontAwesome.otf?alt=media&token=da8d5411-04dd-47a3-a4a8-be76603ca117";
                        File fontFile = getFontFile(context);

                        NetworkUtils.downloadFile(source, fontFile.getAbsolutePath());

                        Log.d(TAG, "File download complete: " + fontFile.getAbsolutePath());

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
        return new File(context.getFilesDir(), "icon-font");
    }
}
