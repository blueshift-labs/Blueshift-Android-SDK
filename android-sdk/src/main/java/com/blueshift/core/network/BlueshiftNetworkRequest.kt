package com.blueshift.core.network

import com.blueshift.core.db.BlueshiftSQLiteModel
import org.json.JSONObject

data class BlueshiftNetworkRequest(
    override val id: Long = -1,
    val url: String,
    val method: Method,
    val headers: Map<String, String> = mapOf("Content-Type" to "application/json"),
    val body: JSONObject? = null,
    var authorization: String? = null, // should add it from network config when needed
    val authorizationRequired: Boolean = false, // for db to store if auth is required
    var retryAttemptBalance: Int = 3,
    var retryAttemptTimestamp: Long = 0, // epoch timestamp
    val timestamp: Long = 0, // epoch timestamp
) : BlueshiftSQLiteModel() {
    enum class Method {
        GET, POST;

        companion object {
            fun fromString(string: String): Method {
                return when (string) {
                    "GET" -> GET
                    "POST" -> POST
                    else -> GET
                }
            }
        }
    }
}
