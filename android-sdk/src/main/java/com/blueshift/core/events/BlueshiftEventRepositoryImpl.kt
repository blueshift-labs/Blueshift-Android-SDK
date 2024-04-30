package com.blueshift.core.events

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.blueshift.core.db.BlueshiftSQLiteOpenHelper
import org.json.JSONObject

class BlueshiftEventRepositoryImpl(
    context: Context?
) : BlueshiftSQLiteOpenHelper<BlueshiftEvent>(
    context, "blueshift_events.sqlite", null, 1
), BlueshiftEventRepository {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(buildCreateTableQuery())
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    override fun getContentValues(obj: BlueshiftEvent): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(ID, obj.id)
        contentValues.put(NAME, obj.eventName)
        contentValues.put(PARAMS, obj.eventParams.toString().toByteArray(charset = Charsets.UTF_8))
        contentValues.put(TIMESTAMP, obj.timestamp)
        return contentValues
    }

    override fun getObject(cursor: Cursor): BlueshiftEvent {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(NAME))
        var paramsJson = JSONObject()
        val paramsByes = cursor.getBlob(cursor.getColumnIndexOrThrow(PARAMS))
        paramsByes?.let {
            val params = String(it, charset = Charsets.UTF_8)
            paramsJson = JSONObject(params)
        }
        val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(TIMESTAMP))

        return BlueshiftEvent(id, name, paramsJson, timestamp)
    }

    override val tableName: String = "events_table"
    override val fields: Map<String, FieldType> = mapOf(
        NAME to FieldType.Text, PARAMS to FieldType.Blob, TIMESTAMP to FieldType.Integer
    )

    companion object {
        private const val NAME = "name"
        private const val PARAMS = "params"
        private const val TIMESTAMP = "timestamp"
    }

    override fun insertEvent(event: BlueshiftEvent) {
        insert(event)
    }

    override fun deleteEvents(events: List<BlueshiftEvent>) {
        val ids = events.joinToString { it.id.toString() } // CSV of ID
        deleteAll(whereClause = "$ID IN (?)", selectionArgs = arrayOf(ids))
    }

    override fun readOneBatch(batchCount: Int): List<BlueshiftEvent> {
        synchronized(this) {
            val events = mutableListOf<BlueshiftEvent>()
            val cursor = readableDatabase.query(
                tableName, null, null, null, null, null, "$TIMESTAMP DESC", "$batchCount"
            )

            while (cursor.moveToNext()) {
                events.add(getObject(cursor))
            }

            cursor.close()

            return events
        }
    }
}