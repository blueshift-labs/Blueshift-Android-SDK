package com.blueshift.core.events

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test

class BlueshiftEventRepositoryImplTest {
    private lateinit var repository: BlueshiftEventRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = BlueshiftEventRepositoryImpl(context)
    }

    @After
    fun tearDown() = runBlocking {
        repository.clear()
    }

    @Test
    fun insertEvent_insertsEventsToTheSQLiteDatabase() = runBlocking {
        val name = "test_event"
        val json = "{\"key\":\"val\"}"
        val timestamp = System.currentTimeMillis()
        val event = BlueshiftEvent(
            eventName = name, eventParams = JSONObject(json), timestamp = timestamp
        )

        repository.insertEvent(event)
        val events = repository.readOneBatch()

        assert(events.size == 1)
        assert(events[0].eventName == name)
        assert(events[0].eventParams.toString() == json)
        assert(events[0].timestamp == timestamp)
    }

    @Test
    fun deleteEvents_deletesEventsFromTheSQLiteDatabase() = runBlocking {
        val name = "test_event"
        val json = "{\"key\":\"val\"}"
        val timestamp = 0L
        val event = BlueshiftEvent(
            eventName = name, eventParams = JSONObject(json), timestamp = timestamp
        )

        repository.insertEvent(event)
        var events = repository.readOneBatch()

        assert(events.size == 1)

        repository.deleteEvents(events)
        events = repository.readOneBatch()

        assert(events.isEmpty())
    }

    @Test
    fun readOneBatch_retrievesAListOfHundredEventsWhenCountIsNotSpecified() = runBlocking {
        for (i in 1..200) {
            val name = "test_event_$i"
            val json = "{\"key\":\"val\"}"
            val timestamp = 0L
            val event = BlueshiftEvent(
                eventName = name, eventParams = JSONObject(json), timestamp = timestamp
            )
            repository.insertEvent(event)
        }

        val events = repository.readOneBatch()
        assert(events.size == 100)
    }

    @Test
    fun readOneBatch_retrievesAListOfTenEventsWhenCountIsSetToTen() = runBlocking {
        for (i in 1..200) {
            val name = "test_event_$i"
            val json = "{\"key\":\"val\"}"
            val timestamp = 0L
            val event = BlueshiftEvent(
                eventName = name, eventParams = JSONObject(json), timestamp = timestamp
            )
            repository.insertEvent(event)
        }

        val events = repository.readOneBatch(batchCount = 10)
        assert(events.size == 10)
    }

    @Test
    fun readOneBatch_retrievesAListOfEventsInTheSameOrderTheyAreStoredInTheDatabase() =
        runBlocking {
            for (i in 1..10) {
                val name = "test_event_$i"
                val json = "{\"key\":\"val\"}"
                val timestamp = 0L
                val event = BlueshiftEvent(
                    eventName = name, eventParams = JSONObject(json), timestamp = timestamp
                )
                repository.insertEvent(event)
            }

            val events = repository.readOneBatch(batchCount = 10)
            for (i in 1..9) {
                assert((events[i].id - events[i - 1].id) == 1L)
            }
        }
}
