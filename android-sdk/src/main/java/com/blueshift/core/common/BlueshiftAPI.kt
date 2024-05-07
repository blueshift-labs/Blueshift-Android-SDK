package com.blueshift.core.common

object BlueshiftAPI {
    sealed class Datacenter(val baseUrl: String) {
        data object EU : Datacenter("https://api.eu.getblueshift.com/")
        data object US : Datacenter("https://api.getblueshift.com/")
    }

    private var currentDatacenter: Datacenter = Datacenter.US

    fun setDatacenter(datacenter: Datacenter) {
        currentDatacenter = datacenter
    }

    private val BASE_URL: String get() = currentDatacenter.baseUrl

    val EVENTS = "${BASE_URL}api/v1/event"
    val BULK_EVENTS = "${BASE_URL}api/v1/bulkevents"
}