package com.blueshift.core.common

import org.junit.Assert.*
import org.junit.Test

class BlueshiftAPITest {
    @Test
    fun setDatacenter_whenNotSet_shouldReturnCorrectURLForAllAPIsWithUSBaseUrl() {
        val baseUrlForUS = "https://api.getblueshift.com/"
        assertEquals("${baseUrlForUS}api/v1/event", BlueshiftAPI.eventURL())
        assertEquals("${baseUrlForUS}api/v1/bulkevents", BlueshiftAPI.bulkEventsURL())
        assertEquals("${baseUrlForUS}track?key=value", BlueshiftAPI.trackURL("key=value"))
    }

    @Test
    fun setDatacenter_whenSetToUS_shouldReturnCorrectURLForAllAPIsWithUSBaseUrl() {
        BlueshiftAPI.setDatacenter(BlueshiftAPI.Datacenter.US)

        val baseUrlForUS = "https://api.getblueshift.com/"
        assertEquals("${baseUrlForUS}api/v1/event", BlueshiftAPI.eventURL())
        assertEquals("${baseUrlForUS}api/v1/bulkevents", BlueshiftAPI.bulkEventsURL())
        assertEquals("${baseUrlForUS}track?key=value", BlueshiftAPI.trackURL("key=value"))
    }

    @Test
    fun setDatacenter_whenSetToEU_shouldReturnCorrectURLForAllAPIsWithEUBaseUrl() {
        BlueshiftAPI.setDatacenter(BlueshiftAPI.Datacenter.EU)

        val baseUrlForEU = "https://api.eu.getblueshift.com/"
        assertEquals("${baseUrlForEU}api/v1/event", BlueshiftAPI.eventURL())
        assertEquals("${baseUrlForEU}api/v1/bulkevents", BlueshiftAPI.bulkEventsURL())
        assertEquals("${baseUrlForEU}track?key=value", BlueshiftAPI.trackURL("key=value"))
    }
}