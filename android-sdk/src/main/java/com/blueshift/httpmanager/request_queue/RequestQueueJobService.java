package com.blueshift.httpmanager.request_queue;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
import com.blueshift.request_queue.RequestQueue;


/**
 * @author Rahul Raveendran V P
 * Created on 13/03/18 @ 11:09 AM
 * https://github.com/rahulrvp
 */


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

        return false;
    }
}
