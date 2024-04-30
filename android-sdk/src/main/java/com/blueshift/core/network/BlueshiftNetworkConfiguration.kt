package com.blueshift.core.network

import android.util.Base64


object BlueshiftNetworkConfiguration {
    var authorization: String? = null
    var requestRetryIntervalInMilliseconds: Long = 5 * (60 * 1000) // 5 Minutes
    var isConnected = true

    fun configureBasicAuthentication(username: String, password: String) {
        val authToken = "$username:$password"
        val base64Token = Base64.encodeToString(authToken.toByteArray(), Base64.NO_WRAP)
        authorization = "Basic ".plus(base64Token)
    }
}