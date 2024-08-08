package com.blueshift.core.common

class BlueshiftAPI {
    enum class Datacenter(val baseUrl: String) {
        US("https://api.getblueshift.com/"), EU("https://api.eu.getblueshift.com/")
    }

    companion object {
        private var region = Datacenter.US

        fun setDatacenter(datacenter: Datacenter) {
            region = datacenter
        }

        fun trackURL(queryString: String): String {
            return "${region.baseUrl}track?$queryString"
        }

        fun eventURL(): String {
            return "${region.baseUrl}api/v1/event"
        }

        fun bulkEventsURL(): String {
            return "${region.baseUrl}api/v1/bulkevents"
        }
    }
}