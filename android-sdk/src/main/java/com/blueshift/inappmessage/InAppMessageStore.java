package com.blueshift.inappmessage;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.blueshift.BlueshiftLogger;
import com.blueshift.framework.BlueshiftBaseSQLiteOpenHelper;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class InAppMessageStore extends BlueshiftBaseSQLiteOpenHelper<InAppMessage> {
    private static final String TAG = InAppMessageStore.class.getSimpleName();
    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "bsft_inappmessage_db";
    private static final String TABLE_NAME = "bsft_inappmessage";

    private static final String ONE = "1";
    private static final String NOW = "now";
    private static final String EMPTY = "";

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_EXPIRES_AT = "expires_at";
    private static final String FIELD_TRIGGER = "trigger";
    private static final String FIELD_DISPLAY_ON = "display_on";
    private static final String FIELD_TEMPLATE_STYLE = "template_style";
    private static final String FIELD_TEMPLATE_STYLE_DARK = "template_style_dark";
    private static final String FIELD_CONTENT_STYLE = "content_style";
    private static final String FIELD_CONTENT_STYLE_DARK = "content_style_dark";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_EXTRAS = "extras";

    private static final String FIELD_MESSAGE_UUID = "message_uuid";
    private static final String FIELD_EXPERIMENT_UUID = "experiment_uuid";
    private static final String FIELD_USER_UUID = "user_uuid";
    private static final String FIELD_TRANSACTION_UUID = "transaction_uuid";
    private static final String FIELD_TIMESTAMP = "timestamp";

    private static final String FIELD_DISPLAYED_AT = "displayed_at";

    private static InAppMessageStore sInstance = null;

    private InAppMessageStore(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * This method maintains the singleton behavior of the class. At the same time, it prevents the
     * possibility of passing null as context into the SQLiteOpenHelper instance. This is to avoid
     * crashes while attempting to read the database path from null mContext variable.
     *
     * @param context valid {@link Context} variable
     * @return {@link InAppMessageStore} object or null (if instance and context are both null)
     */
    @Nullable
    public static InAppMessageStore getInstance(@NonNull Context context) {
        if (sInstance == null && context != null) {
            sInstance = new InAppMessageStore(context.getApplicationContext());
        }

        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(getCreateTableQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        BlueshiftLogger.d(TAG, getTableName() + " -> oldVersion: " + oldVersion + ", newVersion: " + newVersion);

        // we are adding support for dark theme from v2 of this table
        if (oldVersion < 2 && newVersion >= 2) {
            migrateToV2AndAbove(db);
        }
    }

    private void migrateToV2AndAbove(SQLiteDatabase db) {
        // add two columns to the db. template_style_dark and content_style_dark to hold
        // dark theme styles for in-app
        try {
            // template_style_dark
            String queryTSD = "ALTER TABLE " + getTableName() + " ADD COLUMN " + FIELD_TEMPLATE_STYLE_DARK + " " + FieldType.Text;
            db.execSQL(queryTSD);
            BlueshiftLogger.d(TAG, "New column " + FIELD_TEMPLATE_STYLE_DARK + " added in " + getTableName());

            // content_style_dark
            String queryCSD = "ALTER TABLE " + getTableName() + " ADD COLUMN " + FIELD_CONTENT_STYLE_DARK + " " + FieldType.Text;
            db.execSQL(queryCSD);
            BlueshiftLogger.d(TAG, "New column " + FIELD_CONTENT_STYLE_DARK + " added in " + getTableName());
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }
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
            inAppMessage.setMessageUuid(getString(cursor, FIELD_MESSAGE_UUID));
            inAppMessage.setExperimentUuid(getString(cursor, FIELD_EXPERIMENT_UUID));
            inAppMessage.setUserUuid(getString(cursor, FIELD_USER_UUID));
            inAppMessage.setTransactionUuid(getString(cursor, FIELD_TRANSACTION_UUID));
            inAppMessage.setDisplayedAt(getLong(cursor, FIELD_TRANSACTION_UUID));
            inAppMessage.setTimestamp(getString(cursor, FIELD_TIMESTAMP));

            String tsJson = getString(cursor, FIELD_TEMPLATE_STYLE);
            if (!TextUtils.isEmpty(tsJson)) inAppMessage.setTemplateStyle(new JSONObject(tsJson));

            String tsdJson = getString(cursor, FIELD_TEMPLATE_STYLE_DARK);
            if (!TextUtils.isEmpty(tsdJson))
                inAppMessage.setTemplateStyleDark(new JSONObject(tsdJson));

            String csJson = getString(cursor, FIELD_CONTENT_STYLE);
            if (!TextUtils.isEmpty(csJson)) inAppMessage.setContentStyle(new JSONObject(csJson));

            String csdJson = getString(cursor, FIELD_CONTENT_STYLE_DARK);
            if (!TextUtils.isEmpty(csdJson))
                inAppMessage.setContentStyleDark(new JSONObject(csdJson));

            String cJson = getString(cursor, FIELD_CONTENT);
            if (!TextUtils.isEmpty(cJson)) inAppMessage.setContent(new JSONObject(cJson));

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
            values.put(FIELD_TEMPLATE_STYLE_DARK, inAppMessage.getTemplateStyleDarkJson());
            values.put(FIELD_CONTENT_STYLE, inAppMessage.getContentStyleJson());
            values.put(FIELD_CONTENT_STYLE_DARK, inAppMessage.getContentStyleDarkJson());
            values.put(FIELD_CONTENT, inAppMessage.getContentJson());
            values.put(FIELD_MESSAGE_UUID, inAppMessage.getMessageUuid());
            values.put(FIELD_EXPERIMENT_UUID, inAppMessage.getExperimentUuid());
            values.put(FIELD_USER_UUID, inAppMessage.getUserUuid());
            values.put(FIELD_TRANSACTION_UUID, inAppMessage.getTransactionUuid());
            values.put(FIELD_TIMESTAMP, inAppMessage.getTimestamp());
            values.put(FIELD_EXTRAS, inAppMessage.getExtrasJson());
            values.put(FIELD_DISPLAYED_AT, inAppMessage.getDisplayedAt());
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
        fieldTypeHashMap.put(FIELD_TEMPLATE_STYLE_DARK, FieldType.Text);
        fieldTypeHashMap.put(FIELD_CONTENT_STYLE, FieldType.Text);
        fieldTypeHashMap.put(FIELD_CONTENT_STYLE_DARK, FieldType.Text);
        fieldTypeHashMap.put(FIELD_CONTENT, FieldType.Text);
        fieldTypeHashMap.put(FIELD_MESSAGE_UUID, FieldType.UniqueText);
        fieldTypeHashMap.put(FIELD_EXPERIMENT_UUID, FieldType.Text);
        fieldTypeHashMap.put(FIELD_USER_UUID, FieldType.Text);
        fieldTypeHashMap.put(FIELD_TRANSACTION_UUID, FieldType.Text);
        fieldTypeHashMap.put(FIELD_TIMESTAMP, FieldType.Text);
        fieldTypeHashMap.put(FIELD_EXTRAS, FieldType.Text);
        fieldTypeHashMap.put(FIELD_DISPLAYED_AT, FieldType.Long);
        return fieldTypeHashMap;
    }

    InAppMessage getInAppMessage(Activity activity, String screenName) {
        synchronized (_LOCK) {
            InAppMessage inAppMessage = null;

            String className = "unknown";

            if (screenName != null && !screenName.isEmpty()) {
                className = screenName;
            } else if (activity != null) {
                className = activity.getClass().getName();
            }

            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(getTableName());

            StringBuilder where = new StringBuilder();

            // check for the screen to display on
            where.append("(");
            where.append(FIELD_DISPLAY_ON).append("=?");    // ARG #1 className
            where.append(" OR ");
            where.append(FIELD_DISPLAY_ON).append("=?");    // ARG #2 empty('')
            where.append(")");

            where.append(" AND ");

            // check for valid trigger
            where.append("(");
            where.append(FIELD_TRIGGER).append("=?");       // ARG #3 now
            where.append(" OR ");
            where.append(FIELD_TRIGGER).append("<?");       // ARG #4 current time (seconds)
            where.append(" OR ");
            where.append(FIELD_TRIGGER).append("=?");       // ARG #5 empty
            where.append(")");

            // omit displayed items
            where.append(" AND ");
            where.append(FIELD_DISPLAYED_AT).append("=0");

            // omit expired items
            where.append(" AND ");
            where.append(FIELD_EXPIRES_AT).append(">?");    // ARG #6 current time (seconds)

            qb.appendWhere(where);

            String nowSeconds = String.valueOf(System.currentTimeMillis() / 1000);

            String[] selectionArgs = new String[]{
                    className,      // #1
                    EMPTY,          // #2
                    NOW,            // #3
                    nowSeconds,     // #4
                    EMPTY,          // #5
                    nowSeconds      // #6
            };

            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = qb.query(db, null, null, selectionArgs, null, null, FIELD_DISPLAY_ON + " DESC," + _ID + " DESC", ONE);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        inAppMessage = getObject(cursor);
                    }

                    cursor.close();
                }

                db.close();
            }

            return inAppMessage;
        }
    }

    long getLastDisplayedAt() {
        synchronized (_LOCK) {
            long displayedAt = 0;

            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                String[] columns = new String[]{"max(" + FIELD_DISPLAYED_AT + ")"};
                Cursor cursor = db.query(getTableName(), columns, null, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        displayedAt = cursor.getLong(0);
                    }

                    cursor.close();
                }
                db.close();
            }

            return displayedAt;
        }
    }

    String getLastMessageUUID() {
        synchronized (_LOCK) {
            String uuid = null;

            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                String[] columns = new String[]{FIELD_MESSAGE_UUID};
                Cursor cursor = db.query(getTableName(), columns, null, null, null, null, _ID + " DESC", ONE);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        uuid = cursor.getString(0);
                    }

                    cursor.close();
                }

                db.close();
            }

            return uuid;
        }
    }

    InAppMessage getLastInAppMessage() {
        synchronized (_LOCK) {
            InAppMessage inAppMessage = null;

            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                // sort the items based on timestamp string.
                // the greater value will be the recent timestamp.
                Cursor cursor = db.query(getTableName(), null, null, null, null, null, FIELD_TIMESTAMP + " DESC", ONE);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        inAppMessage = getObject(cursor);
                    }

                    cursor.close();
                }

                db.close();
            }

            return inAppMessage;
        }
    }

    public void clean() {
        synchronized (_LOCK) {
            if (getTotalRecordCount() > 40) {
                String days30 = String.valueOf(System.currentTimeMillis() - 2.592e+9); // -30days
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                String whereClause = FIELD_EXPIRES_AT + "<?" + _OR_ + FIELD_DISPLAYED_AT + " BETWEEN 1 AND ?";
                String[] selectionArgs = new String[]{timestamp, days30};

                deleteAll(whereClause, selectionArgs);
            }
        }
    }

    /**
     * Delete all the in-app messages stored in the database.
     */
    public void cleanAll() {
        synchronized (_LOCK) {
            deleteAll(null, null);
        }
    }

    public void markAsRead(List<String> messageUUIDs) {
        synchronized (_LOCK) {
            if (messageUUIDs != null && messageUUIDs.size() > 0) {
                SQLiteDatabase db = getWritableDatabase();
                try {
                    if (db != null) {
                        long timestamp = System.currentTimeMillis() / 1000;
                        ContentValues values = new ContentValues();
                        values.put(FIELD_DISPLAYED_AT, timestamp);

                        boolean first = true;
                        StringBuilder qnMarks = new StringBuilder();
                        for (int i = 0; i < messageUUIDs.size(); i++) {
                            if (first) {
                                first = false;
                            } else {
                                qnMarks.append(",");
                            }

                            qnMarks.append("?");
                        }

                        String whereClause = FIELD_MESSAGE_UUID + " IN (" + qnMarks.toString() + ")";
                        String[] messageUUIDArray = messageUUIDs.toArray(new String[]{});

                        int count = db.update(getTableName(), values, whereClause, messageUUIDArray);

                        BlueshiftLogger.d(TAG, count + " records marked as opened. " + Arrays.toString(messageUUIDArray));

                        db.close();
                    }
                } finally {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                }
            }
        }
    }
}
