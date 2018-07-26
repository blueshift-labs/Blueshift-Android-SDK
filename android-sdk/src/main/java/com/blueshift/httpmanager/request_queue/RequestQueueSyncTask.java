package com.blueshift.httpmanager.request_queue;

import android.content.Context;
import android.os.AsyncTask;


/**
 * @author Rahul Raveendran V P
 *         Created on 13/03/18 @ 11:10 AM
 *         https://github.com/rahulrvp
 */


public class RequestQueueSyncTask extends AsyncTask<Void, Void, Void> {
    private RequestQueue mRequestQueue;
    private Callback mCallback;

    public RequestQueueSyncTask(Context context, Callback callback) {
        mRequestQueue = RequestQueue.getInstance(context);
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        if (mCallback != null) {
            mCallback.onTaskStart();
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (mRequestQueue != null) {
            mRequestQueue.sync();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mCallback != null) {
            mCallback.onTaskComplete();
        }
    }

    public interface Callback {
        void onTaskStart();

        void onTaskComplete();
    }
}
