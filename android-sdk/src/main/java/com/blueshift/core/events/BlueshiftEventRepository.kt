package com.blueshift.core.events

interface BlueshiftEventRepository {
    fun insertEvent(event: BlueshiftEvent)
    fun deleteEvents(events: List<BlueshiftEvent>)
    fun readOneBatch(batchCount: Int = 100) : List<BlueshiftEvent>
}