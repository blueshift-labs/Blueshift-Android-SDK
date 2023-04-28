package com.blueshift.inbox;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.blueshift.BlueshiftLogger;
import com.blueshift.framework.BlueshiftBaseSQLiteOpenHelper;
import com.blueshift.inappmessage.InAppMessage;

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

    private static final String ONE = "1";
    private static final String NOW = "now";
    private static final String EMPTY = "";
    private static final String UNREAD = "unread";

    private static final String COL_ACCOUNT_UUID = "account_uuid";      // account uuid
    private static final String COL_USER_UUID = "user_uuid";            // user uuid
    private static final String COL_MESSAGE_UUID = "message_uuid";      // message uuid
    private static final String COL_DATA = "data";                      // message payload JSON
    private static final String COL_CAMPAIGN_ATTR = "campaign_attr";    // campaign attr JSON
    private static final String COL_CREATED_AT = "created_at";          // epoch timestamp
    private static final String COL_EXPIRES_AT = "expires_at";          // epoch timestamp
    private static final String COL_DELETED_AT = "deleted_at";          // epoch timestamp
    private static final String COL_DISPLAY_ON = "display_on";          // name of screen
    private static final String COL_TRIGGER = "trigger";                // now or timestamp
    private static final String COL_MESSAGE_TYPE = "message_type";      // inapp/push
    private static final String COL_STATUS = "status";                  // read/unread
    private static final String COL_SCOPE = "scope";                    // scope

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

            message.scope = BlueshiftInboxMessage.Scope.fromString(getString(cursor, COL_SCOPE));
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
            contentValues.put(COL_SCOPE, message.scope.toString());
            contentValues.put(COL_DISPLAY_ON, message.displayOn);
            contentValues.put(COL_TRIGGER, message.trigger);
            contentValues.put(COL_MESSAGE_TYPE, message.messageType);
            contentValues.put(COL_STATUS, message.status.toString());
            contentValues.put(COL_CREATED_AT, message.createdAt != null ? message.createdAt.getTime() : 0);
            contentValues.put(COL_EXPIRES_AT, message.expiresAt != null ? message.expiresAt.getTime() : 0);
            contentValues.put(COL_DELETED_AT, message.deletedAt != null ? message.deletedAt.getTime() : 0);
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
        fieldTypeHashMap.put(COL_SCOPE, FieldType.String);
        fieldTypeHashMap.put(COL_DISPLAY_ON, FieldType.String);
        fieldTypeHashMap.put(COL_TRIGGER, FieldType.String);
        fieldTypeHashMap.put(COL_MESSAGE_TYPE, FieldType.String);
        fieldTypeHashMap.put(COL_STATUS, FieldType.String);
        fieldTypeHashMap.put(COL_CREATED_AT, FieldType.Long);
        fieldTypeHashMap.put(COL_EXPIRES_AT, FieldType.Long);
        fieldTypeHashMap.put(COL_DELETED_AT, FieldType.Long);
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
     * Returns the number of unread messages.
     *
     * @return the unread message count.
     */
    @WorkerThread
    public int getUnreadMessageCount() {
        int count = 0;

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(getTableName());

        StringBuilder where = new StringBuilder();
        where.append(COL_STATUS).append("=?");

        qb.appendWhere(where);

        String[] selectionArgs = new String[]{UNREAD};

        synchronized (_LOCK) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = qb.query(db, new String[]{"COUNT(*)"}, null, selectionArgs, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        count = cursor.getInt(0);
                    }

                    cursor.close();
                }
                db.close();
            }
        }

        return count;
    }

    public BlueshiftInboxMessage getMostRecentMessage() {
        BlueshiftInboxMessage inboxMessage = null;

        SQLiteDatabase db = getReadableDatabase();
        if (db != null) {
            Cursor cursor = db.query(getTableName(), null, null, null, null, null, COL_CREATED_AT + " DESC", "1");
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    inboxMessage = getObject(cursor);
                }
                cursor.close();
            }
            db.close();
        }

        return inboxMessage;
    }

    /**
     * Returns next eligible in-app message for display.
     *
     * @param activity   instance of current activity
     * @param screenName name of current screen
     * @return inapp message instance if available, else null
     */
    @WorkerThread
    public InAppMessage getInAppMessage(Activity activity, String screenName) {
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
        where.append(COL_DISPLAY_ON).append("=?");    // ARG #1 className
        where.append(" OR ");
        where.append(COL_DISPLAY_ON).append("=?");    // ARG #2 empty('')
        where.append(")");

        where.append(" AND ");

        // check for valid trigger
        where.append("(");
        where.append(COL_TRIGGER).append("=?");       // ARG #3 now
        where.append(" OR ");
        where.append(COL_TRIGGER).append("<?");       // ARG #4 current time (seconds)
        where.append(" OR ");
        where.append(COL_TRIGGER).append("=?");       // ARG #5 empty
        where.append(")");

        // omit displayed items
        where.append(" AND ");
        where.append(COL_STATUS).append("=?");        // ARG #6 Status (unread)

        // omit expired items
        where.append(" AND ");
        where.append(COL_EXPIRES_AT).append(">?");    // ARG #7 current time (seconds)

        where.append(" AND ");
        // check scope
        where.append("(");
        where.append(COL_SCOPE).append("=?");       // ARG #8 scope=inapp
        where.append(" OR ");
        where.append(COL_SCOPE).append("=?");       // ARG #9 scope=inapp+inbox
        where.append(")");

        qb.appendWhere(where);

        String nowSeconds = String.valueOf(System.currentTimeMillis() / 1000);

        String inappScope = BlueshiftInboxMessage.Scope.INAPP_ONLY.toString();
        String inappInboxScope = BlueshiftInboxMessage.Scope.INBOX_AND_INAPP.toString();

        String[] selectionArgs = new String[]{
                className,       // #1
                EMPTY,           // #2
                NOW,             // #3
                nowSeconds,      // #4
                EMPTY,           // #5
                UNREAD,          // #6
                nowSeconds,      // #7
                inappScope,      // #8
                inappInboxScope  // #9
        };

        synchronized (_LOCK) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = qb.query(db, null, null, selectionArgs, null, null, COL_DISPLAY_ON + " DESC," + _ID + " DESC", ONE);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        try {
                            BlueshiftInboxMessage message = getObject(cursor);
                            inAppMessage = InAppMessage.getInstance(message.data);
                        } catch (Exception ignore) {
                        }
                    }

                    cursor.close();
                }

                db.close();
            }
        }

        return inAppMessage;
    }

    public List<BlueshiftInboxMessage> getInboxMessages() {
        List<BlueshiftInboxMessage> messageList = new ArrayList<>();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(getTableName());

        StringBuilder where = new StringBuilder();
        where.append(COL_SCOPE).append("=?");
        where.append(" OR ");
        where.append(COL_SCOPE).append("=?");

        qb.appendWhere(where);

        String inboxScope = BlueshiftInboxMessage.Scope.INBOX_ONLY.toString();
        String inappInboxScope = BlueshiftInboxMessage.Scope.INBOX_AND_INAPP.toString();
        String[] selectionArgs = new String[]{inboxScope, inappInboxScope};

        synchronized (_LOCK) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = qb.query(db, null, null, selectionArgs, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast()) {
                            messageList.add(getObject(cursor));
                            cursor.moveToNext();
                        }
                    }
                    cursor.close();
                }
                db.close();
            }
        }

        return messageList;
    }

    /**
     * This method is used for deleting messages that are deleted remotely.
     *
     * @param idsToKeep The message uuids of the messages to keep in the db.
     *                  The rest of the messages will be deleted.
     */
    @WorkerThread
    public int deleteMessagesExcept(List<String> idsToKeep) {
        synchronized (_LOCK) {
            int count = 0;
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                if (idsToKeep == null || idsToKeep.isEmpty()) {
                    // delete ALL
                    count = db.delete(getTableName(), null, null);
                    BlueshiftLogger.d(TAG, count + " messages deleted. (All messages in the db)");
                } else {
                    // delete obsolete messages
                    count = db.delete(
                            getTableName(),
                            COL_MESSAGE_UUID + " NOT IN (" + questionMarkCsv(idsToKeep.size()) + ")",
                            idsToKeep.toArray(new String[0])
                    );

                    BlueshiftLogger.d(TAG, count + " messages deleted.");
                }

                db.close();
            }
            return count;
        }
    }

    /**
     * Delete all messages from the database.
     */
    public void deleteAllMessages() {
        synchronized (_LOCK) {
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                db.delete(getTableName(), null, null);
                db.close();
            }
        }
    }

    @WorkerThread
    public void deleteExpiredMessages() {
        synchronized (_LOCK) {
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                long timestamp = System.currentTimeMillis() / 1000;
                // delete obsolete messages
                int count = db.delete(getTableName(), COL_EXPIRES_AT + "<?", new String[]{String.valueOf(timestamp)});
                BlueshiftLogger.d(TAG, count + " expired messages deleted.");
                db.close();
            }
        }
    }

    /**
     * Update the status of messages with provided message ids.
     *
     * @param ids    message ids
     * @param status status value (read, unread)
     */
    @WorkerThread
    public int updateStatus(@NonNull List<String> ids, @NonNull String status) {
        synchronized (_LOCK) {
            int count = 0;
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                if (!ids.isEmpty()) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(COL_STATUS, status);

                    db.update(
                            getTableName(),
                            contentValues, COL_MESSAGE_UUID + " IN (" + questionMarkCsv(ids.size()) + ")",
                            ids.toArray(new String[0])
                    );

                    BlueshiftLogger.d(TAG, count + " messages updated with status '" + status + "'");
                }

                db.close();
            }
            return count;
        }
    }

    public void markMessageAsRead(@NonNull String messageUuid) {
        synchronized (_LOCK) {
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                String read = BlueshiftInboxMessage.Status.READ.toString();

                ContentValues contentValues = new ContentValues();
                contentValues.put(COL_STATUS, read);

                int count = db.update(getTableName(), contentValues, COL_MESSAGE_UUID + "=?", new String[]{messageUuid});
                if (count > 0) {
                    BlueshiftLogger.d(TAG, "message (" + messageUuid + ") updated with status 'read'.");
                } else {
                    BlueshiftLogger.d(TAG, "message (" + messageUuid + ") can not be marked as 'read' or not found in the db.");
                }

                db.close();
            }
        }
    }

    private String questionMarkCsv(int count) {
        StringBuilder qnMarkBuilder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                qnMarkBuilder.append(",");
            }

            qnMarkBuilder.append("?");
        }

        return qnMarkBuilder.toString();
    }

    /**
     * Return the ids of messages stored in the db.
     *
     * @return list of message ids
     */
    @WorkerThread
    public List<String> getStoredMessageIds() {
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
        return getInboxMessages();
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
