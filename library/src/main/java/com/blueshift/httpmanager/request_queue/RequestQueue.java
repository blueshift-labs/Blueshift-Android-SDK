package com.blueshift.httpmanager.request_queue;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.httpmanager.HTTPManager;
import com.blueshift.httpmanager.Request;
import com.blueshift.httpmanager.Response;
import com.blueshift.model.Configuration;

/**
 * Created by rahul on 26/2/15.
 */
public class RequestQueue {
    private static final String LOG_TAG = RequestQueue.class.getSimpleName();
    private static final Boolean lock = true;

    private static final long RETRY_INTERVAL = 5 * 60 * 1000;

    private static Status mStatus;
    private static Context mContext;
    private static RequestQueue mInstance = null;

    private RequestQueue() {
        mStatus = Status.AVAILABLE;
    }

    public synchronized static RequestQueue getInstance(Context context) {
        mContext = context;

        if (mInstance == null) {
            mInstance = new RequestQueue();
        }

        return mInstance;
    }

    public void add(Request request) {
        if (request != null) {
            Log.d(LOG_TAG, "Adding new request to the Queue.");
            RequestQueueTable db = RequestQueueTable.getInstance(mContext);
            db.insert(request);
            db.close();
            sync();
        }
    }

    public void remove(Request request) {
        if (request != null) {
            Log.d(LOG_TAG, "Removing request with id:" + request.getId() + " from the Queue");
            RequestQueueTable db = RequestQueueTable.getInstance(mContext);
            db.delete(request);
            db.close();
        }
    }

    public Request fetch() {
        synchronized (lock) {
            mStatus = Status.BUSY;
            Request request = null;
            RequestQueueTable db = RequestQueueTable.getInstance(mContext);
            Object reqObject = db.getNextRequest();
            if (reqObject != null) {
                request = (Request) reqObject;
            }
            db.close();

            return request;
        }
    }

    public void sync() {
        synchronized (lock) {
            if (mStatus == Status.AVAILABLE && isConnectedToNetwork()) {
                Request request = fetch();
                if (request != null) {
                    if (request.getPendingRetryCount() != 0) {
                        long nextRetryTime = request.getNextRetryTime();
                        // Checks if next retry time had passed or not. (0 is the default time for normal requests.)
                        if (nextRetryTime == 0 || nextRetryTime < System.currentTimeMillis()) {
                            new sendRequestTask(request).execute();
                        } else {
                            // The request has a next retry time which had not passed yet, so we need to move that to back of the queue.
                            remove(request);
                            mStatus = Status.AVAILABLE;
                            add(request);
                        }
                    } else {
                        // Request expired its retries. Need to be removed from queue. This is an escape plan. This case will not happen normally.
                        remove(request);
                        mStatus = Status.AVAILABLE;
                    }
                } else {
                    Log.d(LOG_TAG, "Request queue is empty.");
                    mStatus = Status.AVAILABLE;
                }
            }
        }
    }

    private boolean isConnectedToNetwork() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private class sendRequestTask extends AsyncTask<Void, Void, Boolean> {
        private Request mRequest;

        public sendRequestTask(Request request) {
            mRequest = request;
        }

        @Override
        protected void onPreExecute() {
            mStatus = RequestQueue.Status.BUSY;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mRequest != null) {
                Log.d(LOG_TAG, "Processing request (id: " + mRequest.getId() + ").");
                HTTPManager httpManager = new HTTPManager(mRequest.getUrl());

                Configuration config = Blueshift.getInstance(mContext).getConfiguration();
                if (config != null && config.getApiKey() != null) {
                    httpManager.addBasicAuthentication(config.getApiKey(), "");
                } else {
                    Log.e(LOG_TAG, "Please set a valid API key in your configuration before initialization.");
                }

                Response response = null;
                switch (mRequest.getMethod()) {
                    case POST:
                        // testing log. need to remove this.
                        Log.d(LOG_TAG, "Request params JSON: " + mRequest.getParamJson());
                        response = httpManager.post(mRequest.getParamJson());
                        break;

                    default:
                        Log.e(LOG_TAG, "Unknown method" + mRequest.getMethod());
                }

                if (response != null) {
                    if (response.getStatusCode() / 100 == 2) {
                        Log.d(LOG_TAG, "Request success for request (id: " + mRequest.getId() + "). Status code: " + response.getStatusCode());
                        return true;
                    } else {
                        Log.d(LOG_TAG, "Request failed for request (id: " + mRequest.getId() + "). Status code: " + response.getStatusCode() + ". Response: " + response.getResponseBody());
                        return false;
                    }
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean status) {
            remove(mRequest);
            if (!status) {
                int retryCount = mRequest.getPendingRetryCount() - 1;
                if (retryCount > 0) {
                    mRequest.setPendingRetryCount(retryCount);
                    // setting a retry time 5 minutes from now.
                    long nextRetryTime = (RETRY_INTERVAL) + System.currentTimeMillis();
                    mRequest.setNextRetryTime(nextRetryTime);

                    add(mRequest);
                }
            }
            mStatus = RequestQueue.Status.AVAILABLE;
            sync();
        }
    }

    private enum Status {
        AVAILABLE,
        BUSY
    }
}
