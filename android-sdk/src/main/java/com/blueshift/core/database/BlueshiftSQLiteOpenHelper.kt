package com.blueshift.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import com.blueshift.core.common.BlueshiftLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun insert(t: T): Boolean {
        return withContext(Dispatchers.IO) {
            synchronized(this) {
                val id: Long
                val db = writableDatabase
                id = db.insert(tableName, null, getContentValues(t))

                id != ID_DEFAULT
            }
        }
    }

    suspend fun update(t: T?) {
        withContext(Dispatchers.IO) {
            synchronized(this) {
                val db = writableDatabase
                if (db != null) {
                    if (t != null && t.id > 0) {
                        val count = db.update(tableName, getContentValues(t), "$ID=?", arrayOf("${t.id}"))
                        BlueshiftLogger.d("$TAG: Successfully updated $count record(s) in $tableName where id IN (${t.id})")
                    }
                }
            }
        }
    }

    suspend fun delete(t: T) {
        withContext(Dispatchers.IO) {
            synchronized(this) {
                val db = writableDatabase
                if (db != null) {
                    val id = t?.id
                    val count = db.delete(tableName, "$ID=?", arrayOf("$id"))
                    BlueshiftLogger.d("$TAG: Successfully deleted $count record(s) from $tableName where id IN ($id)")
                }
            }
        }
    }

    suspend fun deleteAll(whereClause: String?, selectionArgs: Array<String?>?) {
        withContext(Dispatchers.IO) {
            synchronized(this) {
                val db = writableDatabase
                if (db != null) {
                    val count = db.delete(tableName, whereClause, selectionArgs)

                    val csv = selectionArgs?.joinToString { it.toString() }
                    BlueshiftLogger.d("$TAG: Successfully deleted $count record(s) from $tableName where id IN ($csv)")
                }
            }
        }
    }

    suspend fun findAll(): List<T> {
        return withContext(Dispatchers.IO) {
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
                }
            }
            records
        }
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
        const val TAG = "SQLiteOpenHelper"
        const val ID_DEFAULT = -1L
        const val ID = "_id"
        const val _AND_ = " AND "
        const val _AND = " AND"
        const val _OR_ = " OR "
        const val _OR = " OR"
    }
}
