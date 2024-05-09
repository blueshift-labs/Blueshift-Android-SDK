package com.blueshift.core.network

import com.blueshift.core.common.BlueshiftLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class BlueshiftNetworkRepositoryImpl : BlueshiftNetworkRepository {

    override suspend fun makeNetworkRequest(networkRequest: BlueshiftNetworkRequest): BlueshiftNetworkResponse {
        return withContext(Dispatchers.IO) {
            with(URL(networkRequest.url)) {
                BlueshiftLogger.d("$TAG - networkRequest = $networkRequest")

                val connection = openConnection() as HttpsURLConnection

                if (networkRequest.authorizationRequired) {
                    val authorization = networkRequest.authorization
                    authorization?.let { connection.setRequestProperty("Authorization", it) }
                }

                networkRequest.headers.let { headers ->
                    headers.forEach { entry ->
                        connection.setRequestProperty(entry.key, entry.value)
                    }
                }

                when (networkRequest.method) {
                    BlueshiftNetworkRequest.Method.GET -> prepareGetRequest(connection)
                    BlueshiftNetworkRequest.Method.POST -> preparePostRequest(
                        connection,
                        networkRequest
                    )
                }

                var response: BlueshiftNetworkResponse
                try {
                    connection.connect()
                    response = readResponseFromHttpsConnection(connection)
                } catch (ex: Exception) {
                    response = BlueshiftNetworkResponse(responseCode = -1, responseBody = "")
                    BlueshiftLogger.e("$TAG - ${ex.stackTraceToString()}")
                } finally {
                    try {
                        connection.disconnect()
                    } catch (_: Exception) {
                    }
                }

                BlueshiftLogger.d("$TAG - networkResponse = $response")

                response
            }
        }
    }

    private fun prepareGetRequest(connection: HttpsURLConnection) {
        connection.requestMethod = "GET"
    }

    private fun preparePostRequest(connection: HttpsURLConnection, request: BlueshiftNetworkRequest) {
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