package com.blueshift.batch;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BulkEventJobService extends JobService {

    private static final String LOG_TAG = BulkEventJobService.class.getSimpleName();

    @Override
    public boolean onStartJob(final JobParameters params) {
        final Context context = this;

        BlueshiftExecutor.getInstance().runOnDiskIOThread(
                new Runnable() {
                    @Override
                    public void run() {
                        BlueshiftLogger.d(LOG_TAG, "Enqueue bulk events started.");
                        BulkEventManager.enqueueBulkEvents(context);
                        BlueshiftLogger.d(LOG_TAG, "Enqueue bulk events completed.");

                        jobFinished(params, true);
                    }
                }
        );

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
