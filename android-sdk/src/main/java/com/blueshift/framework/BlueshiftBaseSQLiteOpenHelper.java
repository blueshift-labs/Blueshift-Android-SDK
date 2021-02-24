package com.blueshift.framework;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class BlueshiftBaseSQLiteOpenHelper<T extends BlueshiftBaseSQLiteModel> extends SQLiteOpenHelper {
    private static final String TAG = BlueshiftBaseSQLiteOpenHelper.class.getSimpleName();
    protected static final String _ID = "_id";
    protected static final Boolean _LOCK = true;
    protected static final String _AND_ = " AND ";
    protected static final String _AND = " AND";
    protected static final String _OR_ = " OR ";
    protected static final String _OR = " OR";

    public BlueshiftBaseSQLiteOpenHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    protected String getCreateTableQuery() {
        String query = null;

        String tableName = getTableName();
        if (!TextUtils.isEmpty(tableName)) {
            HashMap<String, FieldType> fieldTypeMap = getFields();
            if (fieldTypeMap != null && fieldTypeMap.size() > 0) {
                fieldTypeMap.put(_ID, FieldType.Autoincrement);

                StringBuilder createTableQuery = new StringBuilder("CREATE TABLE " + tableName + "(");

                Set<String> fieldNames = fieldTypeMap.keySet();
                boolean isFirstIteration = true;

                for (String fieldName : fieldNames) {
                    if (isFirstIteration) {
                        isFirstIteration = false;
                    } else {
                        createTableQuery.append(",");
                    }

                    String dataType = fieldTypeMap.get(fieldName).toString();
                    createTableQuery.append(fieldName).append(" ").append(dataType);
                }

                query = createTableQuery + ")";

            }
        }

        return query;
    }

    protected long getId(Cursor cursor) {
        return getLong(cursor, _ID);
    }

    protected long getLong(Cursor cursor, String fieldName) {
        return cursor.getLong(cursor.getColumnIndex(fieldName));
    }

    protected String getString(Cursor cursor, String fieldName) {
        return cursor.getString(cursor.getColumnIndex(fieldName));
    }

    public boolean insert(T t) {
        synchronized (_LOCK) {
            long id = -1;
            SQLiteDatabase db = getWritableDatabase();

            if (db != null) {
                id = db.insert(getTableName(), null, getContentValues(t));
                db.close();
            }

            return id != -1;
        }
    }

    public void update(T t) {
        synchronized (_LOCK) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null) {
                if (t != null && t.getId() > 0) {
                    db.update(getTableName(), getContentValues(t), _ID + "=" + t.getId(), null);
                }

                db.close();
            }
        }
    }

    public void delete(T t) {
        synchronized (_LOCK) {
            SQLiteDatabase db = getWritableDatabase();

            if (db != null) {
                db.delete(getTableName(), _ID + "=?", new String[]{String.valueOf(t.getId())});
                db.close();
            }
        }
    }

    protected void deleteAll(String whereClause, String[] selectionArgs) {
        synchronized (_LOCK) {
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                db.delete(getTableName(), whereClause, selectionArgs);
                db.close();
            }
        }
    }

    public long getTotalRecordCount() {
        synchronized (_LOCK) {
            long count = 0;
            SQLiteDatabase db = getWritableDatabase();
            if (db != null) {
                count = DatabaseUtils.queryNumEntries(db, getTableName());
                db.close();
            }
            return count;
        }
    }

    public List<T> findAll() {
        List<T> records = new ArrayList<>();

        synchronized (_LOCK) {
            SQLiteDatabase db = getReadableDatabase();
            if (db != null) {
                Cursor cursor = db.query(getTableName(), null, null, null, null, null, null);
                if (cursor != null) {
                    while (!cursor.isAfterLast()) {
                        records.add(getObject(cursor));
                        cursor.moveToNext();
                    }
                    cursor.close();
                }
                db.close();
            }
        }

        return records;
    }

    abstract protected String getTableName();

    abstract protected T getObject(Cursor cursor);

    abstract protected ContentValues getContentValues(T t);

    abstract protected HashMap<String, FieldType> getFields();

    protected enum FieldType {
        String,
        Text,
        UniqueText,
        Autoincrement,
        Long;

        @Override
        public String toString() {
            switch (this) {
                case String:
                    return "STRING";
                case Text:
                    return "TEXT";
                case UniqueText:
                    return "TEXT NOT NULL UNIQUE";
                case Long:
                    return "INTEGER DEFAULT 0";
                case Autoincrement:
                    return "INTEGER PRIMARY KEY AUTOINCREMENT";
                default:
                    return "";
            }
        }
    }

}
