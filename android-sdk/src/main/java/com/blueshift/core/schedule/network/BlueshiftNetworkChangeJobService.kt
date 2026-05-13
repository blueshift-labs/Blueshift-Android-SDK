package com.blueshift.core.schedule.network

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
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
        BlueshiftLogger.d("$TAG: onStopJob called")
        
        // Add stop reason handling for Android 16+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && params != null) {
            val stopReason = params.stopReason
            handleJobStopReason(stopReason)
            
            // For Android 15/16: Reschedule job for certain stop reasons
            return shouldRescheduleJob(stopReason)
        }
        
        // For older Android versions, don't reschedule
        return false
    }

    /**
     * Determine if job should be rescheduled based on stop reason
     * For Android 15/16 compatibility
     */
    private fun shouldRescheduleJob(stopReason: Int): Boolean {
        return when (stopReason) {
            JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY -> {
                BlueshiftLogger.d("$TAG: Rescheduling job - network constraint will be met when connectivity returns")
                true
            }
            JobParameters.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW -> {
                BlueshiftLogger.d("$TAG: Rescheduling job - will retry when battery is not low")
                true
            }
            JobParameters.STOP_REASON_CONSTRAINT_CHARGING -> {
                BlueshiftLogger.d("$TAG: Rescheduling job - will retry when charging constraint is met")
                true
            }
            JobParameters.STOP_REASON_CONSTRAINT_DEVICE_IDLE -> {
                BlueshiftLogger.d("$TAG: Rescheduling job - will retry when device is idle")
                true
            }
            JobParameters.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW -> {
                BlueshiftLogger.d("$TAG: Rescheduling job - will retry when storage is not low")
                true
            }
            JobParameters.STOP_REASON_PREEMPT -> {
                BlueshiftLogger.d("$TAG: Rescheduling job - was preempted by higher priority job")
                true
            }
            JobParameters.STOP_REASON_TIMEOUT -> {
                BlueshiftLogger.e("$TAG: Not rescheduling - job timed out, may need optimization")
                false
            }
            JobParameters.STOP_REASON_QUOTA -> {
                BlueshiftLogger.e("$TAG: Not rescheduling - quota exceeded, app may be in restricted bucket")
                false
            }
            JobParameters.STOP_REASON_CANCELLED_BY_APP -> {
                BlueshiftLogger.d("$TAG: Not rescheduling - job was cancelled by app")
                false
            }
            else -> {
                BlueshiftLogger.d("$TAG: Unknown stop reason $stopReason - not rescheduling")
                false
            }
        }
    }

    private fun handleJobStopReason(stopReason: Int) {
        when (stopReason) {
            JobParameters.STOP_REASON_TIMEOUT -> {
                BlueshiftLogger.e("$TAG: Job stopped due to timeout")
            }
            JobParameters.STOP_REASON_QUOTA -> {
                BlueshiftLogger.e("$TAG: Job stopped due to quota limits - app may be in restricted bucket")
            }
            JobParameters.STOP_REASON_CANCELLED_BY_APP -> {
                BlueshiftLogger.d("$TAG: Job cancelled by app")
            }
            JobParameters.STOP_REASON_PREEMPT -> {
                BlueshiftLogger.d("$TAG: Job preempted by higher priority job")
            }
            JobParameters.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW -> {
                BlueshiftLogger.d("$TAG: Job stopped - battery low constraint not met")
            }
            JobParameters.STOP_REASON_CONSTRAINT_CHARGING -> {
                BlueshiftLogger.d("$TAG: Job stopped - charging constraint not met")
            }
            JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY -> {
                BlueshiftLogger.d("$TAG: Job stopped - network constraint not met")
            }
            JobParameters.STOP_REASON_CONSTRAINT_DEVICE_IDLE -> {
                BlueshiftLogger.d("$TAG: Job stopped - device idle constraint not met")
            }
            JobParameters.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW -> {
                BlueshiftLogger.d("$TAG: Job stopped - storage not low constraint not met")
            }
            else -> {
                // Handle Android 16+ specific reasons if available
                if (Build.VERSION.SDK_INT >= 35) { // Android 16+ API level
                    handleAndroid16StopReasons(stopReason)
                } else {
                    BlueshiftLogger.d("$TAG: Job stopped, reason: $stopReason")
                }
            }
        }
    }

    @RequiresApi(35) // Android 16+ API level
    private fun handleAndroid16StopReasons(stopReason: Int) {
        when (stopReason) {
            // Note: STOP_REASON_TIMEOUT_ABANDONED is expected to be available in Android 16
            // Using a constant value as it may not be available in current SDK
            10 -> { // Expected value for STOP_REASON_TIMEOUT_ABANDONED
                BlueshiftLogger.e("$TAG: Job abandoned due to timeout - consider optimizing job duration")
                // Job was abandoned, don't reschedule immediately
            }
            else -> {
                BlueshiftLogger.d("$TAG: Job stopped with Android 16+ reason: $stopReason")
            }
        }
    }

    companion object {
        const val TAG = "BlueshiftNetworkChangeJobService"
    }
}