package com.blueshift.core.network

import com.blueshift.core.common.BlueshiftLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class BlueshiftNetworkRepositoryImpl : BlueshiftNetworkRepository {

    override suspend fun makeNetworkRequest(networkRequest: BlueshiftNetworkRequest): BlueshiftNetworkResponse {
        return withContext(Dispatchers.IO) {
            var response: BlueshiftNetworkResponse
            var connection: HttpsURLConnection? = null

            try {
                val url = URL(networkRequest.url)
                connection = url.openConnection() as HttpsURLConnection

                BlueshiftLogger.d("$TAG: $networkRequest")

                if (networkRequest.authorizationRequired) {
                    val authorization = networkRequest.authorization
                    authorization?.let { connection.setRequestProperty("Authorization", it) }
                }

                networkRequest.header?.let { headers ->
                    headers.keys().forEach { key ->
                        connection.setRequestProperty(key, headers.optString(key))
                    }
                }

                when (networkRequest.method) {
                    BlueshiftNetworkRequest.Method.GET -> prepareGetRequest(connection)
                    BlueshiftNetworkRequest.Method.POST -> preparePostRequest(
                        connection, networkRequest
                    )
                }

                connection.connect()
                response = readResponseFromHttpsConnection(connection)
            } catch (e: Exception) {
                response = when (e) {
                    is IOException -> {
                        BlueshiftNetworkResponse(responseCode = 0, responseBody = "IOException")
                    }

                    else -> {
                        BlueshiftNetworkResponse(responseCode = -1, responseBody = "${e.message}")
                    }
                }
            } finally {
                connection?.let {
                    try {
                        it.disconnect()
                    } catch (_: Exception) {
                    }
                }
            }

            BlueshiftLogger.d("$TAG: $response")

            response
        }
    }

    private fun prepareGetRequest(connection: HttpsURLConnection) {
        connection.requestMethod = "GET"
    }

    private fun preparePostRequest(
        connection: HttpsURLConnection, request: BlueshiftNetworkRequest
    ) {
        connection.doOutput = true
        connection.requestMethod = "POST"

        request.body?.let { bodyJson ->
            val bodyBytes = bodyJson.toString().toByteArray()
            val outputStream = connection.outputStream
            outputStream.write(bodyBytes)
            outputStream.flush()
        }
    }

    private fun readResponseFromHttpsConnection(connection: HttpsURLConnection): BlueshiftNetworkResponse {
        val responseCode = connection.responseCode

        val responseBody = try {
            val inputStream = connection.inputStream
            inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            try {
                val errorStream = connection.errorStream
                errorStream.bufferedReader().readText()
            } catch (ex: Exception) {
                BlueshiftLogger.d("$TAG - Error reading error stream: $ex")
                ""
            }
        }


        return BlueshiftNetworkResponse(responseCode = responseCode, responseBody = responseBody)
    }

    companion object {
        private const val TAG = "NetworkRepository"
    }
}