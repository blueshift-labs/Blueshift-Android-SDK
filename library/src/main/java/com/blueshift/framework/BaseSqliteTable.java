package com.blueshift.framework;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.batch.EventsTable;
import com.blueshift.httpmanager.request_queue.RequestQueueTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by asif on 21/11/13.
 */
public abstract class BaseSqliteTable<T> extends SQLiteOpenHelper {
    private static final Boolean lock = true;

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "blueshift_db.sqlite3";
    private static final String LOG_TAG = "BaseSqliteTable";

    private Context mContext;

    public BaseSqliteTable(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create table for - RequestQueue
        String createTableRequestQueue = RequestQueueTable.getInstance(getContext()).generateCreateTableQuery();
        if (!TextUtils.isEmpty(createTableRequestQueue)) {
            db.execSQL(createTableRequestQueue);
        }

        // create table for - Events
        String createTableEvent = EventsTable.getInstance(getContext()).generateCreateTableQuery();
        if (!TextUtils.isEmpty(createTableEvent)) {
            db.execSQL(createTableEvent);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE " + RequestQueueTable.TABLE_NAME);
        db.execSQL("DROP TABLE " + EventsTable.TABLE_NAME);

        onCreate(db);
    }

    /**
     * Generates SQL query to create a table with available tableName and field-Type HashMap.
     *
     * @return valid create table SQL query if valid arguments given, else null.
     */
    protected String generateCreateTableQuery() {
        String query = null;

        String tableName = getTableName();
        HashMap<String, FieldType> fieldTypeMap = getFields();

        if (!TextUtils.isEmpty(tableName) && fieldTypeMap != null && fieldTypeMap.size() != 0) {
            String createTableQuery = "CREATE TABLE " + tableName + "(";

            Set<String> fieldNames = fieldTypeMap.keySet();
            boolean isFirstIteration = true;

            for (String fieldName : fieldNames) {
                if (isFirstIteration) {
                    isFirstIteration = false;
                } else {
                    createTableQuery += ",";
                }

                String dataType = fieldTypeMap.get(fieldName).toString();
                createTableQuery += (fieldName + " " + dataType);
            }

            query = createTableQuery + ")";
        }

        return query;
    }

    public Context getContext() {
        return mContext;
    }

    public void insert(T item) {
        synchronized (lock) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null) {
                db.insert(getTableName(), null, getContentValues(item));
                db.close();
            }
        }
    }

    public void update(T item) {
        synchronized (lock) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null && getId(item) != null) {
                db.update(getTableName(), getContentValues(item), "id=" + getId(item), null);
                db.close();
            }
        }
    }

    protected void delete(String key, String value) {
        synchronized (lock) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null) {
                db.delete(getTableName(), key + "=?", new String[]{value});
                db.close();
            }
        }
    }

    protected int delete(String key, ArrayList<String> values) {
        int count = 0;

        if (!TextUtils.isEmpty(key) && values != null && values.size() > 0) {
            String[] valuesArray = new String[values.size()];
            values.toArray(valuesArray);

            count = delete(key, valuesArray);
        }

        return count;
    }

    protected int delete(String fieldName, String[] values) {
        int count = 0;

        if (!TextUtils.isEmpty(fieldName) && values != null && values.length > 0) {
            String valuesCSV = TextUtils.join(",", values);
            if (!TextUtils.isEmpty(valuesCSV)) {
                synchronized (lock) {
                    SQLiteDatabase db = getWritableDatabase();
                    if (db != null) {
                        Log.d(LOG_TAG, "Deleting records with '" + fieldName + "' IN (" + valuesCSV + ")");
                        count = db.delete(getTableName(), fieldName + " IN (" + valuesCSV + ")", null);
                        db.close();
                    }
                }
            }
        }

        return count;
    }

    public void deleteAll() {
        synchronized (lock) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null) {
                db.delete(getTableName(), null, null);
                db.close();
            }
        }
    }

    /**
     * finder methods
     **/

    public ArrayList<T> findAll() {
        ArrayList<T> result = new ArrayList<>();

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = db.query(getTableName(), null, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        result.add(loadObject(cursor));
                        cursor.moveToNext();
                    }
                }
            }
        }

        return result;
    }

    public ArrayList<T> findWithLimit(int limit) {
        ArrayList<T> result = new ArrayList<>();

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = db.rawQuery("SELECT * FROM " + getTableName() + " LIMIT " + limit, null);
                if (cursor != null) {
                    cursor.moveToFirst();

                    while (!cursor.isAfterLast()) {
                        result.add(loadObject(cursor));
                        cursor.moveToNext();
                    }

                    cursor.close();
                }

                db.close();
            }
        }

        return result;
    }

    public T findByField(String fieldName, String value) {
        T object = null;

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();

            if (db != null) {
                Cursor cursor = db.query(getTableName(), null, fieldName + "='" + value + "'", null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    object = loadObject(cursor);
                }
                db.close();
            }
        }

        return object;
    }

    public T findByField(String fieldName, long value) {
        T object = null;

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();

            if (db != null) {
                Cursor cursor = db.query(getTableName(), null, fieldName + "=" + value, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    object = loadObject(cursor);
                }
                db.close();
            }
        }

        return object;
    }

    public T findRandomByField(String fieldName, long value) {
        T object = null;

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();

            if (db != null) {
                Cursor cursor = db.query(getTableName(), null, fieldName + "=" + value, null, null, null, "RANDOM()");
                if (cursor != null) {
                    cursor.moveToFirst();
                    object = loadObject(cursor);
                }
                db.close();
            }
        }

        return object;
    }

    public ArrayList<T> findAllByField(String fieldName, String value) {
        ArrayList<T> result = new ArrayList<>();

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = db.query(getTableName(), null, fieldName + "='" + value + "'", null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        result.add(loadObject(cursor));
                        cursor.moveToNext();
                    }
                }
            }
        }

        return result;
    }

    public ArrayList<T> findAllByField(String[] fieldNames, String[] values) {
        ArrayList<T> result = new ArrayList<>();

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                String selection = fieldNames[0] + "=?";
                for (int i = 1; i < fieldNames.length; i++) {
                    selection = selection + " AND " + fieldNames[i] + "=?";
                }

                Cursor cursor = db.query(getTableName(), null, selection, values, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        result.add(loadObject(cursor));
                        cursor.moveToNext();
                    }
                }
            }
        }

        return result;
    }


    public ArrayList<T> findAllByField(String[] fieldNames, String[] values, int pageNo, String order) {
        ArrayList<T> result = new ArrayList<>();

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                String selection = fieldNames[0] + "=?";
                for (int i = 1; i < fieldNames.length; i++) {
                    selection = selection + " AND " + fieldNames[i] + "=?";
                }

                Cursor cursor = db.query(getTableName(), null, selection, values, null, null, order, String.valueOf(100 * pageNo));
                if (cursor != null) {
                    cursor.moveToPosition(100 * (pageNo - 1));
                    while (!cursor.isAfterLast()) {
                        result.add(loadObject(cursor));
                        cursor.moveToNext();
                    }
                }
            }
        }

        return result;
    }

    public ArrayList<T> findAllByField(String[] fieldNames, String[] values, int pageNo, String order, String search) {
        ArrayList<T> result = new ArrayList<>();

        synchronized (lock) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                String selection = fieldNames[0] + "=?";
                for (int i = 1; i < fieldNames.length; i++) {
                    selection = selection + " AND " + fieldNames[i] + "=?";
                }
                selection = selection + " AND " + order + " LIKE '%" + search + "%'";

                Cursor cursor = db.query(getTableName(), null, selection, values, null, null, order, String.valueOf(100 * pageNo));
                if (cursor != null) {
                    cursor.moveToPosition(100 * (pageNo - 1));
                    while (!cursor.isAfterLast()) {
                        result.add(loadObject(cursor));
                        cursor.moveToNext();
                    }
                }
            }
        }

        return result;
    }

    public T getFirstRecord() {
        synchronized (lock) {
            ArrayList<T> results = findAll();
            if (results != null && results.size() > 0) {
                return results.get(0);
            }
        }

        return null;
    }

    abstract protected String getTableName();

    abstract protected T loadObject(Cursor cursor);

    abstract protected ContentValues getContentValues(T t);

    abstract protected HashMap<String, FieldType> getFields();

    abstract protected Long getId(T t);

    protected enum FieldType {
        String,
        Autoincrement,
        Long;

        @Override
        public String toString() {
            switch (this) {
                case String:
                    return "STRING";
                case Long:
                    return "INTEGER";
                case Autoincrement:
                    return "INTEGER PRIMARY KEY AUTOINCREMENT";
                default:
                    return "";
            }
        }
    }
}
