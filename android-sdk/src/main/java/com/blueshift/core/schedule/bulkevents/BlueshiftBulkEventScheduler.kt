package com.blueshift.core.schedule.bulkevents

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.model.Configuration
import com.blueshift.util.CommonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BlueshiftBulkEventScheduler {
    fun schedule(context: Context, configuration: Configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scheduleWithJobScheduler(context = context, configuration = configuration)
        } else {
            scheduleWithAlarmManager(context = context, configuration = configuration)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun scheduleWithJobScheduler(context: Context, configuration: Configuration) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobID = configuration.bulkEventsJobId

        // Check if the job exists already, if yes, skip scheduling a new one
        val jobExists = jobScheduler.allPendingJobs.any { it.id == jobID }
        if (jobExists) return

        val intervalMillis = configuration.batchInterval
        val componentName = ComponentName(context, BlueshiftBulkEventJobService::class.java)

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
                BlueshiftLogger.d("job = BlueshiftBulkEventJobService, isScheduled = $isScheduled")
            } catch (e: Exception) {
                BlueshiftLogger.e(e.stackTraceToString())
            }
        }
    }

    private fun scheduleWithAlarmManager(context: Context, configuration: Configuration) {
        // stop the scheduled alarm (if any)
        stopAlarm(context = context)
        // schedule the alarm
        startAlarm(context = context, intervalMillis = configuration.batchInterval)
    }

    private fun pendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, BlueshiftBulkEventAlarmReceiver::class.java)
        intent.`package` = context.packageName
        return PendingIntent.getBroadcast(
            context, 0, intent, CommonUtils.appendImmutableFlag(flags)
        )
    }

    private fun startAlarm(context: Context, intervalMillis: Long) {
        val startAtMillis = SystemClock.elapsedRealtime() + intervalMillis
        BlueshiftLogger.d("Attempting to schedule an alarm! {startAtMillis = $startAtMillis, intervalMillis = $intervalMillis}")
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            alarmManager?.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                startAtMillis,
                intervalMillis,
                pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } catch (e: Exception) {
            BlueshiftLogger.e(e.stackTraceToString())
        }
    }

    private fun stopAlarm(context: Context) {
        // FLAG_NO_CREATE: if described PendingIntent does not already exist,
        // then simply return null instead of creating it.
        val pi = pendingIntent(context = context, flags = PendingIntent.FLAG_NO_CREATE)
        if (pi != null) {
            BlueshiftLogger.d("Attempting to cancelling the scheduled alarm.")
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
                alarmManager?.cancel(pi)
                pi.cancel()
            } catch (e: Exception) {
                BlueshiftLogger.e(e.stackTraceToString())
            }
        }
    }
}