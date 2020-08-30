package com.blueshift.httpmanager.request_queue;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.blueshift.BlueshiftLogger;
import com.blueshift.request_queue.RequestQueue;


/**
 * @author Rahul Raveendran V P
 *         Created on 13/03/18 @ 11:09 AM
 *         https://github.com/rahulrvp
 */


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RequestQueueJobService extends JobService {
    private static final String TAG = "RequestQueueJobService";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        BlueshiftLogger.d(TAG, "Job started.");
        doBackgroundWork(jobParameters);

        // The job should continue running as the enqueue operation may take a while
        // we have the jobFinished method called once the task is complete.
        return true;
    }

    private void doBackgroundWork(final JobParameters jobParameters) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                RequestQueue.getInstance().sync(getApplicationContext());

                // this is a periodic job, we don't need the job scheduler to
                // reschedule this with available backoff policy. hence passing
                // false as 2nd argument.
                jobFinished(jobParameters, false);

                BlueshiftLogger.d(TAG, "Job completed.");
            }
        }).start();
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        BlueshiftLogger.d(TAG, "Job cancel requested.");

        return false;
    }

}
