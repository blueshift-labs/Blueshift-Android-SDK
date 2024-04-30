package com.blueshift.core.network

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.blueshift.core.db.BlueshiftSQLiteOpenHelper
import org.json.JSONObject

class BlueshiftNetworkRequestRepositoryImpl(
    context: Context?
) : BlueshiftSQLiteOpenHelper<BlueshiftNetworkRequest>(
    context, "blueshift_request_queue.sqlite", null, 1
), BlueshiftNetworkRequestRepository {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(buildCreateTableQuery())
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    override fun getContentValues(obj: BlueshiftNetworkRequest): ContentValues {
        val contentValues = ContentValues()

        if (obj.id >= 0) contentValues.put(ID, obj.id)
        contentValues.put(URL, obj.url)
        contentValues.put(METHOD, obj.method.name)
        obj.body?.let {
            contentValues.put(BODY, obj.body.toString().toByteArray(charset = Charsets.UTF_8))
        }
        contentValues.put(AUTH_REQUIRED, if (obj.authorizationRequired) 1 else 0)
        contentValues.put(RETRY_BALANCE, obj.retryAttemptBalance)
        contentValues.put(RETRY_TIMESTAMP, obj.retryAttemptTimestamp)
        contentValues.put(TIMESTAMP, obj.timestamp)

        return contentValues
    }

    override fun getObject(cursor: Cursor): BlueshiftNetworkRequest {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(ID))
        val url = cursor.getString(cursor.getColumnIndexOrThrow(URL))
        val method = cursor.getString(cursor.getColumnIndexOrThrow(METHOD))
        var bodyJson: JSONObject? = null
        val bodyBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(BODY))
        bodyBytes?.let {
            val bodyString = String(it, Charsets.UTF_8)
            bodyJson = JSONObject(bodyString)
        }
        val authRequired = cursor.getInt(cursor.getColumnIndexOrThrow(AUTH_REQUIRED)) == 1
        val retryBalance = cursor.getInt(cursor.getColumnIndexOrThrow(RETRY_BALANCE))
        val retryTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(RETRY_TIMESTAMP))
        val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(TIMESTAMP))

        return BlueshiftNetworkRequest(
            id = id,
            url = url,
            method = BlueshiftNetworkRequest.Method.fromString(method),
            body = bodyJson,
            authorizationRequired = authRequired,
            retryAttemptBalance = retryBalance,
            retryAttemptTimestamp = retryTimestamp,
            timestamp = timestamp
        )
    }

    override val tableName: String = "request_queue_table"
    override val fields: Map<String, FieldType> = mapOf(
        URL to FieldType.String,
        METHOD to FieldType.String,
        BODY to FieldType.Blob,
        AUTH_REQUIRED to FieldType.Integer,
        RETRY_BALANCE to FieldType.Integer,
        RETRY_TIMESTAMP to FieldType.Integer,
        TIMESTAMP to FieldType.Integer,
    )

    companion object {
        private const val URL = "url"
        private const val METHOD = "method"
        private const val BODY = "body"
        private const val AUTH_REQUIRED = "auth_required"
        private const val RETRY_BALANCE = "retry_balance"
        private const val RETRY_TIMESTAMP = "retry_timestamp"
        private const val TIMESTAMP = "timestamp"
    }

    override fun insertRequest(networkRequest: BlueshiftNetworkRequest) {
        insert(networkRequest)
    }

    override fun updateRequest(networkRequest: BlueshiftNetworkRequest) {
        update(networkRequest)
    }

    override fun deleteRequest(networkRequest: BlueshiftNetworkRequest) {
        delete(networkRequest)
    }

    override fun readNextRequest(): BlueshiftNetworkRequest? {
        synchronized(this) {
            var request: BlueshiftNetworkRequest? = null
            val cursor = readableDatabase.query(
                tableName,
                null,
                "$RETRY_BALANCE > 0 AND $RETRY_TIMESTAMP < ${System.currentTimeMillis()}",
                null,
                null,
                null,
                "$TIMESTAMP ASC",
                "1"
            )

            if (cursor.moveToFirst()) {
                request = getObject(cursor)
            }

            cursor.close()

            return request
        }
    }

}