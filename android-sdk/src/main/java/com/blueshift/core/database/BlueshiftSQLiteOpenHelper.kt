package com.blueshift.core.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils

abstract class BlueshiftSQLiteOpenHelper<T : BlueshiftSQLiteModel?>(
    context: Context?, name: String?, factory: CursorFactory?, version: Int
) : SQLiteOpenHelper(context, name, factory, version) {
    protected fun buildCreateTableQuery(): String? {
        var query: String? = null
        val tableName = tableName
        if (!TextUtils.isEmpty(tableName)) {
            val fieldTypeMap = fields.toMutableMap()
            if (fieldTypeMap.isNotEmpty()) {
                fieldTypeMap[ID] = FieldType.Autoincrement
                val createTableQuery = StringBuilder("CREATE TABLE $tableName(")
                val fieldNames: Set<String> = fieldTypeMap.keys
                var isFirstIteration = true
                for (fieldName in fieldNames) {
                    if (isFirstIteration) {
                        isFirstIteration = false
                    } else {
                        createTableQuery.append(",")
                    }
                    val dataType = fieldTypeMap[fieldName].toString()
                    createTableQuery.append(fieldName).append(" ").append(dataType)
                }
                query = "$createTableQuery)"
            }
        }
        return query
    }

    protected fun getId(cursor: Cursor): Long {
        return getLong(cursor, ID)
    }

    protected fun getLong(cursor: Cursor, fieldName: String?): Long {
        val index = cursor.getColumnIndex(fieldName)
        return if (index >= 0) cursor.getLong(index) else 0
    }

    protected fun getString(cursor: Cursor, fieldName: String?): String? {
        val index = cursor.getColumnIndex(fieldName)
        return if (index >= 0) cursor.getString(index) else null
    }

    fun insert(t: T): Boolean {
        synchronized(this) {
            var id: Long = -1
            val db = writableDatabase
            if (db != null) {
                id = db.insert(tableName, null, getContentValues(t))
                db.close()
            }
            return id != -1L
        }
    }

    fun insertOrReplace(tList: List<T>) {
        synchronized(this) {
            val db = writableDatabase
            if (db != null) {
                db.beginTransaction()
                for (t in tList) {
                    db.insertWithOnConflict(
                        tableName, null, getContentValues(t), SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                db.setTransactionSuccessful()
                db.endTransaction()
                db.close()
            }
        }
    }

    fun update(t: T?) {
        synchronized(this) {
            val db = writableDatabase
            if (db != null) {
                if (t != null && t.id > 0) {
                    db.update(
                        tableName, getContentValues(t), "$ID=?", arrayOf("${t.id}")
                    )
                }
                db.close()
            }
        }
    }

    fun delete(t: T) {
        synchronized(this) {
            val db = writableDatabase
            if (db != null) {
                if (t != null) {
                    db.delete(
                        tableName, "$ID=?", arrayOf("${t.id}")
                    )
                }
                db.close()
            }
        }
    }

    protected fun deleteAll(whereClause: String?, selectionArgs: Array<String?>?) {
        synchronized(this) {
            val db = writableDatabase
            if (db != null) {
                db.delete(tableName, whereClause, selectionArgs)
                db.close()
            }
        }
    }

    val totalRecordCount: Long
        get() {
            synchronized(this) {
                var count: Long = 0
                val db = writableDatabase
                if (db != null) {
                    count = DatabaseUtils.queryNumEntries(db, tableName)
                    db.close()
                }
                return count
            }
        }

    fun findAll(): List<T> {
        val records: MutableList<T> = ArrayList()
        synchronized(this) {
            val db = readableDatabase
            if (db != null) {
                val cursor = db.query(tableName, null, null, null, null, null, null)
                if (cursor != null) {
                    cursor.moveToFirst()
                    while (!cursor.isAfterLast) {
                        records.add(getObject(cursor))
                        cursor.moveToNext()
                    }
                    cursor.close()
                }
                db.close()
            }
        }
        return records
    }

    protected abstract fun getObject(cursor: Cursor): T
    protected abstract fun getContentValues(obj: T): ContentValues
    protected abstract val tableName: String
    protected abstract val fields: Map<String, FieldType>

    protected enum class FieldType {
        String, Blob, Text, UniqueText, Autoincrement, Integer;

        override fun toString(): kotlin.String {
            return when (this) {
                String -> "STRING"
                Blob -> "BLOB"
                Text -> "TEXT"
                UniqueText -> "TEXT NOT NULL UNIQUE"
                Integer -> "INTEGER DEFAULT 0"
                Autoincrement -> "INTEGER PRIMARY KEY AUTOINCREMENT"
            }
        }
    }

    companion object {
        const val ID = "_id"
        const val _AND_ = " AND "
        const val _AND = " AND"
        const val _OR_ = " OR "
        const val _OR = " OR"
    }
}
