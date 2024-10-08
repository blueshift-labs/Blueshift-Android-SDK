package com.blueshift.request_queue;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import com.blueshift.BlueshiftExecutor;
import com.blueshift.BlueshiftLogger;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.request_queue.RequestQueueJobService;
import com.blueshift.model.Configuration;
import com.blueshift.util.BlueshiftUtils;
import com.blueshift.util.CommonUtils;
import com.blueshift.util.NetworkUtils;

/**
 * @author Rahul Raveendran V P
 * Created on 26/2/15 @ 3:07 PM
 * https://github.com/rahulrvp
 *
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
public class RequestQueue {
    public static final int DEFAULT_RETRY_COUNT = 3;

    private static final String LOG_TAG = RequestQueue.class.getSimpleName();
    private static final Boolean lock = true;

    private Status mStatus;
    private static RequestQueue mInstance = null;

    public static void scheduleQueueSyncJob(Context context) {
        try {
            if (context != null) {
                Configuration config = BlueshiftUtils.getConfiguration(context);
                if (config != null) {
                    int jobId = config.getNetworkChangeListenerJobId();
                    boolean isJobPending = CommonUtils.isJobPending(context, jobId);
                    if (isJobPending) return; // the job is already scheduled, skip the below code.

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        final JobScheduler jobScheduler
                                = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

                        if (jobScheduler != null) {
                            @SuppressLint("JobSchedulerService")
                            ComponentName componentName = new ComponentName(context, RequestQueueJobService.class);

                            JobInfo.Builder builder = new JobInfo.Builder(jobId, componentName);

                            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
                            builder.setPeriodic(config.getBatchInterval()); // 30 min batch interval by default

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                builder.setRequiresBatteryNotLow(true);
                            }

                            final JobInfo jobInfo = builder.build();

                            BlueshiftExecutor.getInstance().runOnNetworkThread(() -> {
                                try {
                                    if (JobScheduler.RESULT_SUCCESS == jobScheduler.schedule(jobInfo)) {
                                        BlueshiftLogger.d(LOG_TAG, "Job scheduled successfully! (Request Queue Job)");
                                    } else {
                                        BlueshiftLogger.w(LOG_TAG, "Job scheduling failed! (Request Queue Job)");
                                    }
                                } catch (Exception e) {
                                    BlueshiftLogger.e(LOG_TAG, e);
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    private RequestQueue() {
        markQueueAvailable();
    }

    public static RequestQueue getInstance() {
        synchronized (lock) {
            if (mInstance == null) mInstance = new RequestQueue();

            return mInstance;
        }
    }

    public void add(Context context, Request request) {
        synchronized (lock) {
            try {
                if (request != null) {
                    BlueshiftLogger.d(LOG_TAG, "Adding new request to the Queue.");
                    RequestQueueTable.getInstance(context).insert(request);
                }
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        }

        sync(context);
    }

    void remove(Context context, Request request) {
        synchronized (lock) {
            try {
                if (request != null) {
                    BlueshiftLogger.d(LOG_TAG, "Removing request with id:" + request.getId() + " from the Queue");
                    RequestQueueTable.getInstance(context).delete(request);
                }
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        }
    }

    private Request fetch(Context context) {
        synchronized (lock) {
            try {
                markQueueBusy();
                return RequestQueueTable.getInstance(context).getNextRequest();
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
                return null;
            }
        }
    }

    void markQueueAvailable() {
        mStatus = Status.AVAILABLE;
    }

    private void markQueueBusy() {
        mStatus = Status.BUSY;
    }

    public void syncInBackground(final Context context) {
        if (context != null) {
            BlueshiftExecutor.getInstance().runOnDiskIOThread(new Runnable() {
                @Override
                public void run() {
                    sync(context);
                }
            });
        }
    }

    public void sync(final Context context) {
        synchronized (lock) {
            if (mStatus == Status.AVAILABLE && NetworkUtils.isConnected(context)) {
                Request request = fetch(context);
                if (request != null) {
                    if (request.getPendingRetryCount() != 0) {
                        long nextRetryTime = request.getNextRetryTime();
                        // Checks if next retry time had passed or not.
                        // (0 is the default time for normal requests.)
                        if (nextRetryTime == 0 || nextRetryTime < System.currentTimeMillis()) {
                            RequestDispatcher dispatcher = new RequestDispatcher.Builder()
                                    .setContext(context)
                                    .setRequest(request)
                                    .setCallback(new RequestDispatcher.Callback() {
                                        @Override
                                        public void onDispatchBegin() {
                                            // empty listener method
                                        }

                                        @Override
                                        public void onDispatchComplete() {
                                            syncInBackground(context);
                                        }
                                    })
                                    .build();

                            dispatcher.dispatch();
                        } else {
                            // The request has a next retry time which had not passed yet,
                            // so we need to move that to back of the queue.
                            remove(context, request);
                            markQueueAvailable();
                            add(context, request);
                        }
                    } else {
                        // Request expired its retries. Need to be removed from queue.
                        // This is an escape plan. This case will not happen normally.
                        remove(context, request);
                        markQueueAvailable();
                    }
                } else {
                    BlueshiftLogger.d(LOG_TAG, "Request queue is empty.");

                    markQueueAvailable();
                }
            }
        }
    }

    private enum Status {
        AVAILABLE,
        BUSY
    }
}
