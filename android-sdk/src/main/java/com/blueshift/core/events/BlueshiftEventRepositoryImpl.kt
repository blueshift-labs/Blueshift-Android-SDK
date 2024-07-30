package com.blueshift.core.events

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.blueshift.core.database.BlueshiftSQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BlueshiftEventRepositoryImpl(
    context: Context?
) : BlueshiftSQLiteOpenHelper<BlueshiftEvent>(
    context, "com.blueshift.events.db", null, 1
), BlueshiftEventRepository {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(buildCreateTableQuery())
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    override fun getContentValues(obj: BlueshiftEvent): ContentValues {
        val contentValues = ContentValues()
        if (obj.id != ID_DEFAULT) contentValues.put(ID, obj.id)
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

    override suspend fun insertEvent(event: BlueshiftEvent) {
        insert(event)
    }

    override suspend fun deleteEvents(events: List<BlueshiftEvent>) {
        if (events.isEmpty()) return

        val placeholder = StringBuilder()
        val ids = mutableListOf<String>()

        for (event in events) {
            ids.add("${event.id}")
            placeholder.append("?, ")
        }

        // delete the tailing coma and space
        placeholder.delete(placeholder.length - 2, placeholder.length)

        deleteAll(whereClause = "$ID IN ($placeholder)", selectionArgs = ids.toTypedArray())
    }

    override suspend fun readOneBatch(batchCount: Int): List<BlueshiftEvent> {
        return withContext(Dispatchers.IO) {
            synchronized(this) {
                val events = mutableListOf<BlueshiftEvent>()
                val cursor = readableDatabase.query(
                    tableName, null, null, null, null, null, "$TIMESTAMP ASC", "$batchCount"
                )

                while (cursor.moveToNext()) {
                    events.add(getObject(cursor))
                }

                cursor.close()

                events
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            deleteAll(whereClause = null, selectionArgs = null)
        }
    }
}