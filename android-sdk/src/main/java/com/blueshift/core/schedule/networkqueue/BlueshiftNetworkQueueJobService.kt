package com.blueshift.core.schedule.networkqueue

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
import com.blueshift.core.BlueshiftNetworkQueueManager
import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BlueshiftNetworkQueueJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        doBackgroundWork(jobParameters = params)

        // The job should continue running as the enqueue operation may take a while
        // we have the jobFinished method called once the task is complete.
        return true
    }

    private fun doBackgroundWork(jobParameters: JobParameters?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BlueshiftLogger.d("NetworkQueueManager.sync - START")
                if (NetworkUtils.isConnected(applicationContext)) {
                    BlueshiftNetworkQueueManager.sync()
                }
                BlueshiftLogger.d("NetworkQueueManager.sync - START")
            } catch (e: Exception) {
                BlueshiftLogger.e("NetworkQueueManager.sync - Exception: ${e.stackTraceToString()}")
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
}