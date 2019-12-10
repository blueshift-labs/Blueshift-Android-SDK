package com.blueshift;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BlueshiftExecutor {

    private static BlueshiftExecutor sInstance = null;
    private final Executor uiExecutor;
    private final Executor diskExecutor;
    private final Executor networkExecutor;

    private BlueshiftExecutor(Executor uiExecutor, Executor diskExecutor, Executor networkExecutor) {
        this.uiExecutor = uiExecutor;
        this.diskExecutor = diskExecutor;
        this.networkExecutor = networkExecutor;
    }

    public static BlueshiftExecutor getInstance() {
        if (sInstance == null) {
            sInstance = new BlueshiftExecutor(
                    new UIExecutor(),
                    Executors.newSingleThreadExecutor(),
                    Executors.newFixedThreadPool(3)
            );
        }

        return sInstance;
    }

    public void runOnMainThread(Runnable runnable) {
        if (uiExecutor != null) {
            uiExecutor.execute(runnable);
        }
    }

    public void runOnNetworkThread(Runnable runnable) {
        if (networkExecutor != null) {
            networkExecutor.execute(runnable);
        }
    }

    public void runOnDiskIOThread(Runnable runnable) {
        if (diskExecutor != null) {
            diskExecutor.execute(runnable);
        }
    }

    private static class UIExecutor implements Executor {
        private final Handler uiHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable runnable) {
            uiHandler.post(runnable);
        }
    }
}
