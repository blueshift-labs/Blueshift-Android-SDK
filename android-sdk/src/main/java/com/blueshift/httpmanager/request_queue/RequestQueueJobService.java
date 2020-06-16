package com.blueshift.httpmanager.request_queue;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.blueshift.request_queue.RequestQueue;


/**
 * @author Rahul Raveendran V P
 *         Created on 13/03/18 @ 11:09 AM
 *         https://github.com/rahulrvp
 */


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RequestQueueJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        RequestQueue.getInstance().syncInBackground(this);
        // this method needs to be called to release the wakelock
        jobFinished(jobParameters, false);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

}
