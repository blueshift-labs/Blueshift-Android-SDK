package com.blueshift.core

import com.blueshift.core.common.BlueshiftLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object BlueshiftLambdaQueue {
    private val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        coroutineScope.launch {
            for (block in channel) {
                try {
                    block()
                } catch (e: Exception) {
                    BlueshiftLogger.e(e.stackTraceToString())
                }
            }
        }
    }

    fun push(block: suspend () -> Unit) {
        channel.trySend {
            runBlocking {
                block()
            }
        }.isSuccess
    }
}
