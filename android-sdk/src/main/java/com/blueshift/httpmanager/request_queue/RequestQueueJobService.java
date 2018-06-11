package com.blueshift.httpmanager.request_queue;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;


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
                getApplicationContext(),
                new RequestQueueSyncTask.Callback() {
                    @Override
                    public void onTaskStart() {
                        Log.i(LOG_TAG, "db sync task started.");
                    }

                    @Override
                    public void onTaskComplete() {
                        Log.i(LOG_TAG, "db sync task complete.");

                        // this method needs to be called to release the wakelock
                        jobFinished(jobParameters, false);
                    }
                });

        mReqQueueSyncTask.execute();

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
