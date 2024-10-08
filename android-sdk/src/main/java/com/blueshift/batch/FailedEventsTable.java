package com.blueshift.batch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.blueshift.BlueshiftLogger;
import com.blueshift.framework.BaseSqliteTable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Rahul on 24/8/16.
 * <p>
 * This class is created to store all failed high priority events.
 * These events will be taken to build the batch first.
 * The values from {@link EventsTable} will only be taken if this table has no entries,
 * or if this table contains less number of events than a max batch size.
 *
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
public class FailedEventsTable extends BaseSqliteTable<Event> {

    public static final String TABLE_NAME = "failed_events";

    private static final String LOG_TAG = "FailedEventsTable";
    private static final String FIELD_ID = "id";
    private static final String FIELD_EVENT_PARAMS_JSON = "event_params_json";

    private static FailedEventsTable sInstance;

    public FailedEventsTable(Context context) {
        super(context);
    }

    public static FailedEventsTable getInstance(Context context) {
        synchronized (lock) {
            if (sInstance == null) {
                sInstance = new FailedEventsTable(context);
            }

            return sInstance;
        }
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected Event loadObject(Cursor cursor) {
        Event event = new Event();

        try {
            if (isValidCursor(cursor)) {
                event.setId(getLong(cursor, FIELD_ID));

                String json = getString(cursor, FIELD_EVENT_PARAMS_JSON);
                HashMap<String, Object> paramsMap = new HashMap<>();
                if (!TextUtils.isEmpty(json)) {
                    Type type = new TypeToken<HashMap<String, Object>>() {
                    }.getType();
                    paramsMap = new Gson().fromJson(json, type);
                }
                event.setEventParams(paramsMap);
            }
        } catch (Exception e) {
            event = null;
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return event;
    }

    @Override
    protected ContentValues getContentValues(Event event) {
        ContentValues contentValues = new ContentValues();

        if (event != null) {
            contentValues.put(FIELD_EVENT_PARAMS_JSON, event.getEventParamsJson());
        }

        return contentValues;
    }

    @Override
    protected HashMap<String, FieldType> getFields() {
        HashMap<String, FieldType> fieldTypeHashMap = new HashMap<>();

        fieldTypeHashMap.put(FIELD_ID, FieldType.Autoincrement);
        fieldTypeHashMap.put(FIELD_EVENT_PARAMS_JSON, FieldType.String);

        return fieldTypeHashMap;
    }

    @Override
    protected Long getId(Event event) {
        return event != null ? event.getId() : 0;
    }

    /**
     * Bulk event api takes max 1MB of events.
     * This method is used for generating an array of event request parameter JSONs.
     * This method will retrieve and delete the first batchSize amount of records from db. Then will create
     * an ArrayList of Strings, where each element will be on event request's parameters as JSON.
     *
     * @return array of event request parameter JSONs
     */
    public ArrayList<HashMap<String, Object>> getBulkEventParameters(int batchSize) {
        synchronized (lock) {
            try {
                ArrayList<HashMap<String, Object>> result = new ArrayList<>();

                ArrayList<Event> events = new ArrayList<>();
                SQLiteDatabase readableDatabase = getReadableDatabase();
                try {
                    if (readableDatabase != null) {
                        Cursor cursor = readableDatabase.query(
                                getTableName(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                String.valueOf(batchSize));
                        if (cursor != null) {
                            if (cursor.moveToFirst()) {
                                do {
                                    events.add(loadObject(cursor));
                                } while (cursor.moveToNext());
                            }

                            cursor.close();
                        }

                        readableDatabase.close();
                    }
                } finally {
                    if (readableDatabase != null && readableDatabase.isOpen()) {
                        readableDatabase.close();
                    }
                }

                if (events.size() > 0) {
                    ArrayList<String> idList = new ArrayList<>();

                    for (Event event : events) {
                        // get the event parameter
                        result.add(event.getEventParams());

                        // get id for deleting the item.
                        idList.add(String.valueOf(event.getId()));
                    }

                    if (idList.size() > 0) {
                        int count = 0;

                        String valuesCSV = TextUtils.join(",", idList);
                        SQLiteDatabase writableDatabase = getWritableDatabase();
                        try {
                            if (writableDatabase != null) {
                                BlueshiftLogger.d(LOG_TAG, "Deleting records from '" + getTableName() + "' with '" + FIELD_ID + "' IN (" + valuesCSV + ")");

                                count = writableDatabase.delete(getTableName(), FIELD_ID + " IN (" + valuesCSV + ")", null);
                                writableDatabase.close();
                            }
                        } finally {
                            if (writableDatabase != null && writableDatabase.isOpen()) {
                                writableDatabase.close();
                            }
                        }

                        BlueshiftLogger.i(LOG_TAG, "Deleted " + count + " events.");
                    }
                }

                return result;
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
                // return null to make sure no data is being sent to server and crash
                // is not leading to any data leakage.
                return new ArrayList<>(0);
            }
        }
    }
}
