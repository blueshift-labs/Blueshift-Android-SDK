package com.blueshift.httpmanager.request_queue;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
import com.blueshift.request_queue.RequestQueue;


/**
 * @author Rahul Raveendran V P
 * Created on 13/03/18 @ 11:09 AM
 * https://github.com/rahulrvp
 *
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RequestQueueJobService extends JobService {
    private static final String TAG = "RequestQueueJobService";

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        BlueshiftExecutor.getInstance().runOnNetworkThread(new Runnable() {
            @Override
            public void run() {
                doBackgroundWork(jobParameters);
            }
        });

        // The job should continue running as the enqueue operation may take a while
        // we have the jobFinished method called once the task is complete.
        return true;
    }

    private void doBackgroundWork(JobParameters jobParameters) {
        try {
            BlueshiftLogger.d(TAG, "Job started.");
            Context appContext = getApplicationContext();
            RequestQueue.getInstance().sync(appContext);
            BlueshiftLogger.d(TAG, "Job completed.");
        } catch (Exception ignore) {
        }

        // this is a periodic job, we don't need the job scheduler to
        // reschedule this with available backoff policy. hence passing
        // false as 2nd argument.
        jobFinished(jobParameters, false);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        BlueshiftLogger.d(TAG, "Job cancel requested.");
        
        // Add Android 16+ stop reason handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && jobParameters != null) {
            int stopReason = jobParameters.getStopReason();
            handleJobStopReason(stopReason);
        }

        return false;
    }

    private void handleJobStopReason(int stopReason) {
        switch (stopReason) {
            case JobParameters.STOP_REASON_TIMEOUT:
                BlueshiftLogger.w(TAG, "Request queue job stopped due to timeout");
                break;
            case JobParameters.STOP_REASON_QUOTA:
                BlueshiftLogger.w(TAG, "Request queue job stopped due to quota limits");
                break;
            case JobParameters.STOP_REASON_CANCELLED_BY_APP:
                BlueshiftLogger.d(TAG, "Request queue job cancelled by app");
                break;
            case JobParameters.STOP_REASON_PREEMPT:
                BlueshiftLogger.d(TAG, "Request queue job preempted by higher priority job");
                break;
            case JobParameters.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW:
                BlueshiftLogger.d(TAG, "Request queue job stopped - battery low constraint not met");
                break;
            case JobParameters.STOP_REASON_CONSTRAINT_CHARGING:
                BlueshiftLogger.d(TAG, "Request queue job stopped - charging constraint not met");
                break;
            case JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY:
                BlueshiftLogger.d(TAG, "Request queue job stopped - network constraint not met");
                break;
            case JobParameters.STOP_REASON_CONSTRAINT_DEVICE_IDLE:
                BlueshiftLogger.d(TAG, "Request queue job stopped - device idle constraint not met");
                break;
            case JobParameters.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW:
                BlueshiftLogger.d(TAG, "Request queue job stopped - storage not low constraint not met");
                break;
            default:
                // Handle Android 16+ specific reasons if available
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    handleAndroid16StopReasons(stopReason);
                } else {
                    BlueshiftLogger.d(TAG, "Request queue job stopped, reason: " + stopReason);
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void handleAndroid16StopReasons(int stopReason) {
        switch (stopReason) {
            case 10: // Expected value for STOP_REASON_TIMEOUT_ABANDONED in Android 16
                BlueshiftLogger.w(TAG, "Request queue job abandoned - consider optimizing request processing");
                break;
            default:
                BlueshiftLogger.d(TAG, "Request queue job stopped with Android 16+ reason: " + stopReason);
                break;
        }
    }
}
