package com.blueshift.util;

import android.content.Context;

/**
 * Created by rahul on 11/3/15.
 */
public class StorageUtils {

    public static void saveStringInPrefStore(Context context, String fileName, String key, String value) {
        String prefFileName = context.getPackageName() + "." + fileName;
        String prefKey = context.getPackageName() + "." + key;

        context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)
                .edit()
                .putString(prefKey, value)
                .commit();
    }

    public static String getStringFromPrefStore(Context context, String fileName, String key) {
        String prefFileName = context.getPackageName() + "." + fileName;
        String prefKey = context.getPackageName() + "." + key;

        return context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)
                .getString(prefKey, null);
    }
}
