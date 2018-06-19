package com.blueshift.batch;

import android.content.Context;
import android.os.AsyncTask;

public class BulkEventEnqueueTask extends AsyncTask<Context, Void, Void> {
    private Callback mCallback;

    BulkEventEnqueueTask(Callback callback) {
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        if (mCallback != null) {
            mCallback.onStartTask();
        }
    }

    @Override
    protected Void doInBackground(Context... contexts) {
        BulkEventManager.enqueueBulkEvents(contexts[0]);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mCallback != null) {
            mCallback.onStopTask();
        }
    }

    interface Callback {
        void onStartTask();

        void onStopTask();
    }
}
