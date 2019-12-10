package com.blueshift.httpmanager.request_queue;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.blueshift.util.SdkLog;


/**
 * @author Rahul Raveendran V P
 *         Created on 13/03/18 @ 11:09 AM
 *         https://github.com/rahulrvp
 */


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RequestQueueJobService extends JobService {
    private final String LOG_TAG = "RequestQueueJobService";

    private RequestQueueSyncTask mReqQueueSyncTask;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        mReqQueueSyncTask = new RequestQueueSyncTask(
                new RequestQueueSyncTask.Callback() {
                    @Override
                    public void onTaskStart() {
                        SdkLog.i(LOG_TAG, "Request queue db sync task started.");
                    }

                    @Override
                    public void onTaskComplete() {
                        SdkLog.i(LOG_TAG, "Request queue db sync task complete.");

                        // this method needs to be called to release the wakelock
                        jobFinished(jobParameters, false);
                    }
                });

        mReqQueueSyncTask.execute(getApplicationContext());

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mReqQueueSyncTask != null) {
            mReqQueueSyncTask.cancel(true);
        }

        return true;
    }
}
