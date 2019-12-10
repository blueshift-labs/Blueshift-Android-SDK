package com.blueshift.batch;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Log;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BulkEventJobService extends JobService {

    private static final String LOG_TAG = BulkEventJobService.class.getSimpleName();
    private BulkEventEnqueueTask mJobTask;

    @Override
    public boolean onStartJob(final JobParameters params) {
        mJobTask = new BulkEventEnqueueTask(new BulkEventEnqueueTask.Callback() {
            @Override
            public void onStartTask() {
                Log.d(LOG_TAG, "Enqueue bulk events started.");
            }

            @Override
            public void onStopTask() {
                Log.d(LOG_TAG, "Enqueue bulk events completed.");

                jobFinished(params, true);
            }
        });
        mJobTask.execute(this);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mJobTask != null) {
            mJobTask.cancel(true);
        }

        return true;
    }
}
