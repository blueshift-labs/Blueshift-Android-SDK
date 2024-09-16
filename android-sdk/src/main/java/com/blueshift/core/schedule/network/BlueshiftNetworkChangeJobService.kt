package com.blueshift.core.schedule.network

import android.app.job.JobParameters
import android.app.job.JobService
import com.blueshift.core.BlueshiftEventManager
import com.blueshift.core.BlueshiftNetworkRequestQueueManager
import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlueshiftNetworkChangeJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        doBackgroundWork(jobParameters = params)

        // The job should continue running as the enqueue operation may take a while
        // we have the jobFinished method called once the task is complete.
        return true
    }

    private fun doBackgroundWork(jobParameters: JobParameters?) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                BlueshiftLogger.d("$TAG: doBackgroundWork - START")

                // Create batches of events and add it to request queue
                BlueshiftEventManager.buildAndEnqueueBatchEvents()

                // If internet is available, sync the queue
                if (NetworkUtils.isConnected(applicationContext)) {
                    BlueshiftNetworkRequestQueueManager.sync()
                }

                BlueshiftLogger.d("$TAG: doBackgroundWork - FINISH")
            } catch (e: Exception) {
                BlueshiftLogger.e("$TAG: doBackgroundWork - ERROR : ${e.stackTraceToString()}")
            }

            // this is a periodic job, we don't need the job scheduler to
            // reschedule this with available backoff policy. hence passing
            // false as 2nd argument.
            jobFinished(jobParameters, false)
        }
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    companion object {
        const val TAG = "BlueshiftNetworkChangeJobService"
    }
}