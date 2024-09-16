package com.blueshift.core.events

import com.blueshift.core.database.BlueshiftSQLiteModel
import org.json.JSONObject

data class BlueshiftEvent(
    override val id: Long = -1,
    val eventName: String,
    val eventParams: JSONObject,
    val timestamp: Long,
) : BlueshiftSQLiteModel()
