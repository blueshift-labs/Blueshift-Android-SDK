package com.blueshift.httpmanager.request_queue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.blueshift.framework.BaseSqliteTable;
import com.blueshift.httpmanager.Method;
import com.blueshift.httpmanager.Request;

import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 26/5/15 @ 3:07 PM
 *         https://github.com/rahulrvp
 */
public class RequestQueueTable extends BaseSqliteTable<Request> {
    private static final String LOG_TAG = RequestQueueTable.class.getSimpleName();

    public static final String TABLE_NAME = "request_queue";

    private static final String FIELD_REQUEST_ID = "id";
    private static final String FIELD_REQUEST_URL = "url";
    private static final String FIELD_REQUEST_METHOD = "method";
    private static final String FIELD_IS_MULTIPART = "multipart";
    private static final String FIELD_REQUEST_FILE_PATH = "file_path";
    private static final String FIELD_REQUEST_PARAMS_JSON = "params_json";
    private static final String FIELD_REQUEST_TYPE = "request_type";
    private static final String FIELD_URL_PARAMS = "url_params";
    private static final String FIELD_PENDING_RETRY_COUNT = "pending_retry_count";
    private static final String FIELD_NEXT_RETRY_TIME = "next_retry_time";

    private static RequestQueueTable sInstance;

    private RequestQueueTable(Context context) {
        super(context);
    }

    public static RequestQueueTable getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RequestQueueTable(context);
        }

        return sInstance;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected Request loadObject(Cursor cursor) {
        Request request = new Request();

        if (cursor != null && !cursor.isAfterLast()) {
            request.setId(cursor.getLong(cursor.getColumnIndex(FIELD_REQUEST_ID)));
            request.setUrl(cursor.getString(cursor.getColumnIndex(FIELD_REQUEST_URL)));
            request.setMethod(Method.valueOf(cursor.getString(cursor.getColumnIndex(FIELD_REQUEST_METHOD))));
            request.setFilePath(cursor.getString(cursor.getColumnIndex(FIELD_REQUEST_FILE_PATH)));
            request.setParamJson(cursor.getString(cursor.getColumnIndex(FIELD_REQUEST_PARAMS_JSON)));
            request.setRequestType(cursor.getString(cursor.getColumnIndex(FIELD_REQUEST_TYPE)));
            request.setUrlParams(cursor.getString(cursor.getColumnIndex(FIELD_URL_PARAMS)));
            request.setPendingRetryCount(cursor.getInt(cursor.getColumnIndex(FIELD_PENDING_RETRY_COUNT)));
            request.setNextRetryTime(cursor.getLong(cursor.getColumnIndex(FIELD_NEXT_RETRY_TIME)));
        }

        return request;
    }

    @Override
    protected ContentValues getContentValues(Request request) {
        ContentValues contentValues = new ContentValues();

        if (request != null) {
            contentValues.put(FIELD_REQUEST_URL, request.getUrl());
            contentValues.put(FIELD_REQUEST_METHOD, request.getMethod().toString());
            contentValues.put(FIELD_REQUEST_FILE_PATH, request.getFilePath());
            contentValues.put(FIELD_REQUEST_PARAMS_JSON, request.getParamJson());
            contentValues.put(FIELD_REQUEST_TYPE, request.getRequestType());
            contentValues.put(FIELD_URL_PARAMS, request.getUrlParamsAsJSON());
            contentValues.put(FIELD_PENDING_RETRY_COUNT, request.getPendingRetryCount());
            contentValues.put(FIELD_NEXT_RETRY_TIME, request.getNextRetryTime());
        }

        return contentValues;
    }

    @Override
    protected HashMap<String, FieldType> getFields() {
        HashMap<String, FieldType> fieldInfo = new HashMap<>();
        fieldInfo.put(FIELD_REQUEST_ID, FieldType.Autoincrement);
        fieldInfo.put(FIELD_REQUEST_URL, FieldType.String);
        fieldInfo.put(FIELD_REQUEST_METHOD, FieldType.String);
        fieldInfo.put(FIELD_REQUEST_FILE_PATH, FieldType.String);
        fieldInfo.put(FIELD_REQUEST_PARAMS_JSON, FieldType.String);
        fieldInfo.put(FIELD_REQUEST_TYPE, FieldType.String);
        fieldInfo.put(FIELD_URL_PARAMS, FieldType.String);
        fieldInfo.put(FIELD_PENDING_RETRY_COUNT, FieldType.Long);
        fieldInfo.put(FIELD_NEXT_RETRY_TIME, FieldType.Long);
        return fieldInfo;
    }

    @Override
    protected Long getId(Request request) {
        if (request != null) {
            return request.getId();
        }
        return null;
    }

    public void delete(Request request) {
        delete(FIELD_REQUEST_ID, String.valueOf(getId(request)));
    }

    public Object getNextRequest() {
        String condition = FIELD_PENDING_RETRY_COUNT + "=0 OR " + FIELD_NEXT_RETRY_TIME + "<" + System.currentTimeMillis();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(getTableName(), null, condition, null, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            return loadObject(cursor);
        }

        return null;
    }

    public void clearAll() {
        deleteAll();
        Log.i(LOG_TAG, "Request queue cleared" );
    }
}
