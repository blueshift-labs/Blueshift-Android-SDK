package com.blueshift.core.events

import kotlin.math.min

class FakeEventsRepo : BlueshiftEventRepository {
     val blueshiftEvents = mutableListOf<BlueshiftEvent>()
    override fun insertEvent(event: BlueshiftEvent) {
        blueshiftEvents.add(event)
    }

    override fun deleteEvents(events: List<BlueshiftEvent>) {
        blueshiftEvents.removeAll(events)
    }

    override fun readOneBatch(batchCount: Int): List<BlueshiftEvent> {
        val result = mutableListOf<BlueshiftEvent>()
        val limit = min(batchCount, blueshiftEvents.size)
        for (i in 0 until limit) {
            result.add(blueshiftEvents[i])
        }

        return result
    }
}