package com.blueshift.httpmanager.request_queue;

import android.content.Context;
import android.os.AsyncTask;

import com.blueshift.request_queue.RequestQueue;


/**
 * @author Rahul Raveendran V P
 *         Created on 13/03/18 @ 11:10 AM
 *         https://github.com/rahulrvp
 */


public class RequestQueueSyncTask extends AsyncTask<Context, Void, Void> {
    private RequestQueue mRequestQueue;
    private Callback mCallback;

    public RequestQueueSyncTask(Callback callback) {
        mRequestQueue = RequestQueue.getInstance();
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        if (mCallback != null) {
            mCallback.onTaskStart();
        }
    }

    @Override
    protected Void doInBackground(Context... contexts) {
        if (mRequestQueue != null && contexts != null && contexts.length > 0) {
            mRequestQueue.sync(contexts[0]);
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
