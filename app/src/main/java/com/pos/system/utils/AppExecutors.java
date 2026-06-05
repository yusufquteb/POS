package com.pos.system.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Centralized executor pool — use instead of raw Thread/AsyncTask.
 *
 * Usage:
 *   executors.diskIO().execute(() -> {
 *       Result r = db.query();
 *       executors.mainThread().execute(() -> liveData.setValue(r));
 *   });
 */
@Singleton
public final class AppExecutors {

    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;

    @Inject
    public AppExecutors() {
        this.diskIO = Executors.newSingleThreadExecutor();
        this.networkIO = Executors.newFixedThreadPool(3);
        this.mainThread = new MainThreadExecutor();
    }

    /** Single background thread for all DB / file operations */
    public Executor diskIO() { return diskIO; }

    /** Thread pool for network requests */
    public Executor networkIO() { return networkIO; }

    /** Posts back to the Android main thread */
    public Executor mainThread() { return mainThread; }

    private static final class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
