package com.blueshift.batch;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.blueshift.BlueshiftLogger;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BulkEventJobService extends JobService {
    private static final String TAG = "BulkEventJobService";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        BlueshiftLogger.d(TAG, "Job started.");
        doBackgroundWork(jobParameters);

        // The job should continue running as the enqueue operation may take a while
        // we have the jobFinished method called once the task is complete.
        return true;
    }

    private void doBackgroundWork(final JobParameters jobParameters) {
        final Context appContext = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BulkEventManager.enqueueBulkEvents(appContext);

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
