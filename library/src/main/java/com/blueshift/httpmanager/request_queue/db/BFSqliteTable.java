package com.blueshift.httpmanager.request_queue.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by asif on 21/11/13.
 */
public abstract class BFSqliteTable extends SQLiteOpenHelper {
    private static final Boolean lock = true;

    public BFSqliteTable(Context context, String name, int version) {
        super(context, name, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + getTableName() + " (";

        HashMap<String, FieldType> fields = getFields();
        for (String key : fields.keySet()) {
            query = query.concat(key + " " + fields.get(key).toString() + ",");
        }

        query = query.substring(0, query.length() - 2); // to remove the last comma
        query = query.concat(")");
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE " + getTableName());
        onCreate(db);
    }

    public void insert(Object item) {
        synchronized (lock) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null) {
                db.insert(getTableName(), null, getContentValues(item));
                db.close();
            }
        }
    }

    public void update(Object item) {
        synchronized (lock) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null && getId(item) != null) {
                db.update(getTableName(), getContentValues(item), "id="+getId(item), null);
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

    public void deleteAll() {
        synchronized (lock) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null) {
                db.delete(getTableName(), null, null);
                db.close();
            }
        }
    }

    /** finder methods **/

    public ArrayList<Object> findAll() {
        ArrayList<Object> result = new ArrayList<Object>();

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

    public Object findByField(String fieldName, String value) {
        Object object = null;

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

    public Object findByField(String fieldName, long value) {
        Object object = null;

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

    public Object findRandomByField(String fieldName, long value) {
        Object object = null;

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

    public ArrayList<Object> findAllByField(String fieldName, String value) {
        ArrayList<Object> result = new ArrayList<Object>();

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

    public ArrayList<Object> findAllByField(String[] fieldNames, String[] values) {
        ArrayList<Object> result = new ArrayList<Object>();

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


    public ArrayList<Object> findAllByField(String[] fieldNames, String[] values, int pageNo, String order) {
        ArrayList<Object> result = new ArrayList<Object>();

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

    public ArrayList<Object> findAllByField(String[] fieldNames, String[] values, int pageNo, String order, String search) {
        ArrayList<Object> result = new ArrayList<Object>();

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

    public Object getFirstRecord() {
        synchronized (lock) {
            ArrayList<Object> results = findAll();
            if (results != null && results.size() > 0) {
                return results.get(0);
            }
        }

        return null;
    }

    abstract protected String getTableName();

    abstract protected Object loadObject(Cursor cursor);

    abstract protected ContentValues getContentValues(Object object);

    abstract protected HashMap<String, FieldType> getFields();

    abstract protected Long getId(Object object);

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
