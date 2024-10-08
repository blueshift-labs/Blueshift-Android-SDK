package com.blueshift.core.schedule.network

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.model.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BlueshiftNetworkChangeScheduler {
    fun scheduleWithJobScheduler(context: Context, configuration: Configuration) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobID = configuration.networkChangeListenerJobId

        // Check if the job exists already, if yes, skip scheduling a new one
        val jobExists = jobScheduler.allPendingJobs.any { it.id == jobID }
        if (jobExists) return

        val intervalMillis = configuration.batchInterval
        val componentName = ComponentName(context, BlueshiftNetworkChangeJobService::class.java)

        val builder = JobInfo.Builder(jobID, componentName)
        // Send events on any network type
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        // Send events on a given interval of time, default being 30 minutes.
        builder.setPeriodic(intervalMillis)
        // Send events only when battery is not low
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setRequiresBatteryNotLow(true)
        }
        val jobInfo = builder.build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isScheduled = jobScheduler.schedule(jobInfo)
                BlueshiftLogger.d("job = BlueshiftNetworkChangeJobService, jobId = $jobID, isScheduled = $isScheduled")
            } catch (e: Exception) {
                BlueshiftLogger.e(e.stackTraceToString())
            }
        }
    }
}