package com.blueshift.core.events

interface BlueshiftEventRepository {
    suspend fun insertEvent(event: BlueshiftEvent)
    suspend fun deleteEvents(events: List<BlueshiftEvent>)
    suspend fun readOneBatch(batchCount: Int = 100) : List<BlueshiftEvent>
}