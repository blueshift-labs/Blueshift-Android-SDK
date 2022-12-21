package com.blueshift.inbox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.blueshift.BlueshiftLogger;
import com.blueshift.framework.BlueshiftBaseSQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class BlueshiftInboxStoreSQLite extends BlueshiftBaseSQLiteOpenHelper<BlueshiftInboxMessage> implements BlueshiftInboxStore {
    private static final String TAG = "InboxStoreSQLite";
    private static BlueshiftInboxStoreSQLite instance;
    private static final String DB_NAME = "bsft_inbox.sqlite3";
    private static final int DB_VERSION = 1;

    private static final String COL_ACCOUNT_UUID = "account_uuid";      // account uuid
    private static final String COL_USER_UUID = "user_uuid";            // user uuid
    private static final String COL_MESSAGE_UUID = "message_uuid";      // message uuid
    private static final String COL_DATA = "data";                      // message payload JSON
    private static final String COL_CAMPAIGN_ATTR = "campaign_attr";    // campaign attr JSON
    private static final String COL_CREATED_AT = "created_at";          // epoch timestamp
    private static final String COL_EXPIRES_AT = "expires_at";          // epoch timestamp
    private static final String COL_DELETED_AT = "deleted_at";          // epoch timestamp
    private static final String COL_DISPLAYED_AT = "displayed_at";      // epoch timestamp
    private static final String COL_DISPLAY_ON = "display_on";          // name of screen
    private static final String COL_TRIGGER = "trigger";                // now or timestamp
    private static final String COL_MESSAGE_TYPE = "message_type";      // inapp/push
    private static final String COL_STATUS = "status";                  // read/unread

    private BlueshiftInboxStoreSQLite(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static BlueshiftInboxStoreSQLite getInstance(Context context) {
        if (instance == null) instance = new BlueshiftInboxStoreSQLite(context);
        return instance;
    }

    @Override
    protected String getTableName() {
        return "inbox_messages";
    }

    @Override
    protected BlueshiftInboxMessage getObject(Cursor cursor) {
        BlueshiftInboxMessage message = new BlueshiftInboxMessage();
        if (cursor != null) {
            message.setId(getId(cursor));
            message.accountId = getString(cursor, COL_ACCOUNT_UUID);
            message.userId = getString(cursor, COL_USER_UUID);
            message.messageId = getString(cursor, COL_MESSAGE_UUID);

            message.displayOn = getString(cursor, COL_DISPLAY_ON);
            message.trigger = getString(cursor, COL_TRIGGER);
            message.messageType = getString(cursor, COL_MESSAGE_TYPE);
            message.status = BlueshiftInboxMessage.Status.fromString(getString(cursor, COL_STATUS));

            long createdAt = getLong(cursor, COL_CREATED_AT);
            if (createdAt > 0) message.createdAt = new Date(createdAt);

            long expiresAt = getLong(cursor, COL_EXPIRES_AT);
            if (expiresAt > 0) message.expiresAt = new Date(expiresAt);

            long deletedAt = getLong(cursor, COL_DELETED_AT);
            if (deletedAt > 0) message.deletedAt = new Date(deletedAt);

            long displayedAt = getLong(cursor, COL_DISPLAYED_AT);
            if (displayedAt > 0) message.displayedAt = new Date(displayedAt);

            String dataJSON = getString(cursor, COL_DATA);
            try {
                if (dataJSON != null) message.data = new JSONObject(dataJSON);
            } catch (JSONException ignore) {
            }

            String campaignAttr = getString(cursor, COL_CAMPAIGN_ATTR);
            try {
                if (campaignAttr != null) message.campaignAttr = new JSONObject(campaignAttr);
            } catch (JSONException ignore) {
            }
        }

        return message;
    }

    @Override
    protected ContentValues getContentValues(BlueshiftInboxMessage message) {
        ContentValues contentValues = new ContentValues();
        if (message != null) {
            contentValues.put(COL_ACCOUNT_UUID, message.accountId);
            contentValues.put(COL_USER_UUID, message.userId);
            contentValues.put(COL_MESSAGE_UUID, message.messageId);
            contentValues.put(COL_DISPLAY_ON, message.displayOn);
            contentValues.put(COL_TRIGGER, message.trigger);
            contentValues.put(COL_MESSAGE_TYPE, message.messageType);
            contentValues.put(COL_STATUS, message.status.toString());
            contentValues.put(COL_CREATED_AT, message.createdAt != null ? message.createdAt.getTime() : 0);
            contentValues.put(COL_EXPIRES_AT, message.expiresAt != null ? message.expiresAt.getTime() : 0);
            contentValues.put(COL_DELETED_AT, message.deletedAt != null ? message.deletedAt.getTime() : 0);
            contentValues.put(COL_DISPLAYED_AT, message.displayedAt != null ? message.displayedAt.getTime() : 0);
            contentValues.put(COL_DATA, message.data != null ? message.data.toString() : null);
            contentValues.put(COL_CAMPAIGN_ATTR, message.campaignAttr != null ? message.campaignAttr.toString() : null);
        }
        return contentValues;
    }

    @Override
    protected HashMap<String, FieldType> getFields() {
        HashMap<String, FieldType> fieldTypeHashMap = new HashMap<>();
        fieldTypeHashMap.put(COL_ACCOUNT_UUID, FieldType.String);
        fieldTypeHashMap.put(COL_USER_UUID, FieldType.String);
        fieldTypeHashMap.put(COL_MESSAGE_UUID, FieldType.UniqueText);
        fieldTypeHashMap.put(COL_DISPLAY_ON, FieldType.String);
        fieldTypeHashMap.put(COL_TRIGGER, FieldType.String);
        fieldTypeHashMap.put(COL_MESSAGE_TYPE, FieldType.String);
        fieldTypeHashMap.put(COL_STATUS, FieldType.String);
        fieldTypeHashMap.put(COL_CREATED_AT, FieldType.Long);
        fieldTypeHashMap.put(COL_EXPIRES_AT, FieldType.Long);
        fieldTypeHashMap.put(COL_DELETED_AT, FieldType.Long);
        fieldTypeHashMap.put(COL_DISPLAYED_AT, FieldType.Long);
        fieldTypeHashMap.put(COL_DATA, FieldType.Text);
        fieldTypeHashMap.put(COL_CAMPAIGN_ATTR, FieldType.Text);
        return fieldTypeHashMap;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(getCreateTableQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    /**
     * This method is used for deleting messages that are deleted remotely.
     *
     * @param idsToKeep The message uuids of the messages to keep in the db.
     *                  The rest of the messages will be deleted.
     */
    public void deleteMessages(List<String> idsToKeep) {
        synchronized (_LOCK) {
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                String idCsv = stringListToCsv(idsToKeep);
                if (idCsv.isEmpty()) {
                    // delete ALL
                    db.delete(getTableName(), null, null);
                } else {
                    // delete obsolete messages
                    int count = db.delete(getTableName(), COL_MESSAGE_UUID + " NOT IN (?)", new String[]{idCsv});
                    BlueshiftLogger.d(TAG, count + " messages deleted.");
                }
                db.close();
            }
        }
    }

    public void updateStatus(List<String> ids, String status) {
        synchronized (_LOCK) {
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                String idCsv = stringListToCsv(ids);
                if (!idCsv.isEmpty()) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(COL_STATUS, status);

                    int count = db.update(getTableName(), contentValues, COL_MESSAGE_UUID + " IN (?)", new String[]{idCsv});
                    BlueshiftLogger.d(TAG, count + " messages updated.");
                }
                db.close();
            }
        }
    }

    private String stringListToCsv(List<String> values) {
        StringBuilder csvBuilder = new StringBuilder();

        boolean first = true;
        for (String value : values) {
            if (first) {
                first = false;
            } else {
                csvBuilder.append(",");
            }

            csvBuilder.append(value);
        }

        return csvBuilder.toString();
    }

    public List<String> getMessageIds() {
        List<String> mids = new ArrayList<>();

        synchronized (_LOCK) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = db.query(getTableName(), new String[]{COL_MESSAGE_UUID}, null, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast()) {
                            String mid = cursor.getString(0);
                            if (mid != null && !mid.isEmpty()) mids.add(mid);
                            cursor.moveToNext();
                        }
                    }
                    cursor.close();
                }

                db.close();
            }
        }

        return mids;
    }

    @WorkerThread
    @Override
    public List<BlueshiftInboxMessage> getMessages() {
        return findAll();
    }

    @Override
    public void addMessages(List<BlueshiftInboxMessage> messages) {
        insertOrReplace(messages);
    }

    @Override
    public void deleteMessage(BlueshiftInboxMessage message) {
        delete(message);
    }

    @Override
    public void updateMessage(BlueshiftInboxMessage message) {
        update(message);
    }
}
