package com.blueshift.core.schedule.network

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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

        // Use adaptive interval based on app standby bucket for Android 15+
        val intervalMillis = getOptimizedInterval(context, configuration.batchInterval)
        val componentName = ComponentName(context, BlueshiftNetworkChangeJobService::class.java)

        val builder = JobInfo.Builder(jobID, componentName)
        // Send events on any network type
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        // Send events on a given interval of time, with adaptive scheduling for Android 15+
        builder.setPeriodic(intervalMillis)
        // Send events only when battery is not low
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setRequiresBatteryNotLow(true)
        }
        
        // Enhanced constraints for better reliability
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        }
        
        val jobInfo = builder.build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isScheduled = jobScheduler.schedule(jobInfo)
                BlueshiftLogger.d("job = BlueshiftNetworkChangeJobService, jobId = $jobID, isScheduled = $isScheduled, interval = ${intervalMillis}ms")
                
                // Debug job status on Android 16+ if scheduling failed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && isScheduled != JobScheduler.RESULT_SUCCESS) {
                    debugJobSchedulingFailure(context, jobID)
                }
            } catch (e: Exception) {
                BlueshiftLogger.e("Failed to schedule job: ${e.stackTraceToString()}")
            }
        }
    }

    /**
     * Get optimized job interval based on app standby bucket for Android 15+
     * This helps adapt to the new JobScheduler quota system
     */
    private fun getOptimizedInterval(context: Context, defaultInterval: Long): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                val appStandbyBucket = usageStatsManager?.getAppStandbyBucket()
                
                return when (appStandbyBucket) {
                    UsageStatsManager.STANDBY_BUCKET_ACTIVE,
                    UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> {
                        BlueshiftLogger.d("App in active/working_set bucket, using default interval")
                        defaultInterval
                    }
                    UsageStatsManager.STANDBY_BUCKET_FREQUENT -> {
                        BlueshiftLogger.d("App in frequent bucket, increasing interval by 50%")
                        (defaultInterval * 1.5).toLong()
                    }
                    UsageStatsManager.STANDBY_BUCKET_RARE -> {
                        BlueshiftLogger.d("App in rare bucket, doubling interval")
                        defaultInterval * 2
                    }
                    UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> {
                        BlueshiftLogger.e("App in restricted bucket, significantly increasing interval")
                        defaultInterval * 4
                    }
                    else -> {
                        BlueshiftLogger.d("Unknown standby bucket, using default interval")
                        defaultInterval
                    }
                }
            } catch (e: Exception) {
                BlueshiftLogger.e("Failed to get app standby bucket: ${e.message}")
                return defaultInterval
            }
        }
        
        return defaultInterval
    }

    /**
     * Debug job scheduling failure on Android 16+
     * Uses new JobScheduler introspection APIs
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun debugJobSchedulingFailure(context: Context, jobId: Int) {
        try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            
            // Note: These APIs are expected in Android 16 but may not be available in current SDK
            // Using reflection as a fallback until the APIs are officially available
            try {
                val getPendingJobReasonsMethod = jobScheduler.javaClass.getMethod("getPendingJobReasons", Int::class.java)
                val reasons = getPendingJobReasonsMethod.invoke(jobScheduler, jobId)
                BlueshiftLogger.e("Job $jobId failed to schedule. Pending reasons: $reasons")
            } catch (e: NoSuchMethodException) {
                BlueshiftLogger.d("JobScheduler introspection APIs not available yet")
            } catch (e: Exception) {
                BlueshiftLogger.e("Failed to get job pending reasons: ${e.message}")
            }
            
            // Check current job quota status
            val allJobs = jobScheduler.allPendingJobs
            BlueshiftLogger.d("Total pending jobs: ${allJobs.size}")
            
        } catch (e: Exception) {
            BlueshiftLogger.e("Failed to debug job scheduling: ${e.message}")
        }
    }
}