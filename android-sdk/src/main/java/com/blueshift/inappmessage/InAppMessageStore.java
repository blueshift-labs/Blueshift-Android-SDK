package com.blueshift.inappmessage;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.BlueshiftLogger;
import com.blueshift.framework.BlueshiftBaseSQLiteOpenHelper;

import org.json.JSONObject;

import java.util.HashMap;

public class InAppMessageStore extends BlueshiftBaseSQLiteOpenHelper<InAppMessage> {
    private static final String TAG = InAppMessageStore.class.getSimpleName();
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "bsft_inappmessage_db";
    private static final String TABLE_NAME = "bsft_inappmessage";
    private static final String LIMIT_ONE = "1";
    private static final String NOW = "now";

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_EXPIRES_AT = "expires_at";
    private static final String FIELD_TRIGGER = "trigger";
    private static final String FIELD_DISPLAY_ON = "display_on";
    private static final String FIELD_TEMPLATE_STYLE = "template_style";
    private static final String FIELD_CONTENT_STYLE = "content_style";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_CAMPAIGN_PARAMS = "campaign_params";
    private static final String FIELD_EXTRAS = "extras";

    private static InAppMessageStore sInstance = null;

    private InAppMessageStore(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static InAppMessageStore getInstance(Context context) {
        if (sInstance == null) sInstance = new InAppMessageStore(context);
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(getCreateTableQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected InAppMessage getObject(Cursor cursor) {
        try {
            InAppMessage inAppMessage = new InAppMessage();
            inAppMessage.setId(getId(cursor));
            inAppMessage.setType(getString(cursor, FIELD_TYPE));
            inAppMessage.setExpiresAt(getLong(cursor, FIELD_EXPIRES_AT));
            inAppMessage.setTrigger(getString(cursor, FIELD_TRIGGER));
            inAppMessage.setDisplayOn(getString(cursor, FIELD_DISPLAY_ON));

            String tsJson = getString(cursor, FIELD_TEMPLATE_STYLE);
            if (!TextUtils.isEmpty(tsJson)) inAppMessage.setTemplateStyle(new JSONObject(tsJson));

            String csJson = getString(cursor, FIELD_CONTENT_STYLE);
            if (!TextUtils.isEmpty(csJson)) inAppMessage.setContentStyle(new JSONObject(csJson));

            String cJson = getString(cursor, FIELD_CONTENT);
            if (!TextUtils.isEmpty(cJson)) inAppMessage.setContent(new JSONObject(cJson));

            String cpJson = getString(cursor, FIELD_CAMPAIGN_PARAMS);
            if (!TextUtils.isEmpty(cpJson)) inAppMessage.setCampaignParams(new JSONObject(cpJson));

            String xJson = getString(cursor, FIELD_EXTRAS);
            if (!TextUtils.isEmpty(xJson)) inAppMessage.setExtras(new JSONObject(xJson));

            return inAppMessage;
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    @Override
    protected ContentValues getContentValues(InAppMessage inAppMessage) {
        ContentValues values = new ContentValues();

        try {
            values.put(FIELD_TYPE, inAppMessage.getType());
            values.put(FIELD_EXPIRES_AT, inAppMessage.getExpiresAt());
            values.put(FIELD_TRIGGER, inAppMessage.getTrigger());
            values.put(FIELD_DISPLAY_ON, inAppMessage.getDisplayOn());
            values.put(FIELD_TEMPLATE_STYLE, inAppMessage.getTemplateStyleJson());
            values.put(FIELD_CONTENT_STYLE, inAppMessage.getContentStyleJson());
            values.put(FIELD_CONTENT, inAppMessage.getContentJson());
            values.put(FIELD_CAMPAIGN_PARAMS, inAppMessage.getCampaignParamsJson());
            values.put(FIELD_EXTRAS, inAppMessage.getExtrasJson());
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return values;
    }

    @Override
    protected HashMap<String, FieldType> getFields() {
        HashMap<String, FieldType> fieldTypeHashMap = new HashMap<>();
        fieldTypeHashMap.put(FIELD_TYPE, FieldType.Text);
        fieldTypeHashMap.put(FIELD_EXPIRES_AT, FieldType.Long);
        fieldTypeHashMap.put(FIELD_TRIGGER, FieldType.Text);
        fieldTypeHashMap.put(FIELD_DISPLAY_ON, FieldType.Text);
        fieldTypeHashMap.put(FIELD_TEMPLATE_STYLE, FieldType.Text);
        fieldTypeHashMap.put(FIELD_CONTENT_STYLE, FieldType.Text);
        fieldTypeHashMap.put(FIELD_CONTENT, FieldType.Text);
        fieldTypeHashMap.put(FIELD_CAMPAIGN_PARAMS, FieldType.Text);
        fieldTypeHashMap.put(FIELD_EXTRAS, FieldType.Text);
        return fieldTypeHashMap;
    }

    InAppMessage getNextInAppMessage(Activity activity) {
        synchronized (_LOCK) {
            InAppMessage inAppMessage = null;
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                if (activity != null) {
                    String className = activity.getClass().getName();

                    // take IAM with current screen as display time and trigger as 'now' and has less expiry time
                    String selection = FIELD_DISPLAY_ON + "=?" + " AND " + FIELD_TRIGGER + "=?";
                    String[] selectionArgs = new String[]{className, NOW};
                    Cursor cursor = db.query(getTableName(), null, selection, selectionArgs, null, null, FIELD_EXPIRES_AT, LIMIT_ONE);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            Log.d(TAG, "IAM Found. Selection: " + String.format(selection, className, NOW));
                            inAppMessage = getObject(cursor);
                        }

                        cursor.close();
                    }

                    if (inAppMessage == null) {
                        // take IAM with current screen as display time and has less expiry time
                        String selection2 = FIELD_DISPLAY_ON + "=?";
                        String[] selectionArgs2 = new String[]{className};
                        Cursor cursor2 = db.query(getTableName(), null, selection2, selectionArgs2, null, null, FIELD_EXPIRES_AT, LIMIT_ONE);
                        if (cursor2 != null) {
                            if (cursor2.moveToFirst()) {
                                Log.d(TAG, "IAM Found. Selection: " + String.format(selection, className));
                                inAppMessage = getObject(cursor2);
                            }

                            cursor2.close();
                        }
                    }
                }

                if (inAppMessage == null) {
                    // take IAM with current screen as display time and has less expiry time
                    String selection = FIELD_TRIGGER + "=?";
                    String[] selectionArgs = new String[]{NOW};
                    Cursor cursor = db.query(getTableName(), null, selection, selectionArgs, null, null, FIELD_EXPIRES_AT, LIMIT_ONE);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            Log.d(TAG, "IAM Found. Selection: " + String.format(selection, NOW));
                            inAppMessage = getObject(cursor);
                        }

                        cursor.close();
                    }
                }

                if (inAppMessage == null) {
                    String selection = FIELD_DISPLAY_ON + " IS NULL" + _OR_ + FIELD_DISPLAY_ON + "=?";
                    String[] selectionArgs = new String[]{"''"};
                    // take IAM with current screen as display time and has less expiry time
                    Cursor cursor = db.query(getTableName(), null, selection, selectionArgs, null, null, FIELD_EXPIRES_AT, LIMIT_ONE);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            Log.d(TAG, "IAM Found. Selection: " + String.format(selection, "''"));
                            inAppMessage = getObject(cursor);
                        }

                        cursor.close();
                    }
                }

                db.close();
            }

            return inAppMessage;
        }
    }

    public void clean() {
        if (getTotalRecordCount() > 30) {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String whereClause = FIELD_EXPIRES_AT + "<?";
            String[] selectionArgs = new String[]{timestamp};

            deleteAll(whereClause, selectionArgs);
        }
    }
}
