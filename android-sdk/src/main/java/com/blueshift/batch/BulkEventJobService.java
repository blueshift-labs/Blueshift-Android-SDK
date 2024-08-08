package com.blueshift.batch;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;

/**
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BulkEventJobService extends JobService {
    private static final String TAG = "BulkEventJobService";

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
            BulkEventManager.enqueueBulkEvents(appContext);
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
