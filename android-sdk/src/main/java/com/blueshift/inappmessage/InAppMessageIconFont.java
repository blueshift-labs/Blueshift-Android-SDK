package com.blueshift.inappmessage;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.widget.TextView;

import com.blueshift.BlueshiftLogger;

public class InAppMessageIconFont {
    private static final String TAG = "InAppMessageIconFont";
    private static Typeface sFontAwesome5FreeSolid900 = null;
    private static InAppMessageIconFont sInstance = null;

    private InAppMessageIconFont(Context context) {
        try {
            if (context != null) {
                AssetManager assetManager = context.getAssets();
                if (assetManager != null) {
                    sFontAwesome5FreeSolid900 = Typeface.createFromAsset(
                            assetManager, "Font Awesome 5 Free-Solid-900.otf");
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
    }

    public static InAppMessageIconFont getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new InAppMessageIconFont(context);
        }

        return sInstance;
    }

    public void apply(TextView textView) {
        if (textView != null && sFontAwesome5FreeSolid900 != null) {
            textView.setTypeface(sFontAwesome5FreeSolid900);
        }
    }
}
