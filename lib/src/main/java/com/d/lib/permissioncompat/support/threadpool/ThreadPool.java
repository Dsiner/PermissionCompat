package com.d.lib.permissioncompat.support.threadpool;

import android.support.annotation.NonNull;

/**
 * Abstract thread pool, you can also implement it yourself,
 * the default implementation here is TaskManager
 * Created by D on 2018/8/25.
 */
public abstract class ThreadPool {
    private volatile static ThreadPool INSTANCE;

    protected ThreadPool() {
    }

    public static void setThreadPool(ThreadPool pool) {
        synchronized (ThreadPool.class) {
            if (ThreadPool.INSTANCE == null) {
                // Initialize only once
                ThreadPool.INSTANCE = pool;
            }
        }
    }

    public static ThreadPool getInstance() {
        if (INSTANCE == null) {
            // Not implemented, then use the default
            synchronized (ThreadPool.class) {
                if (INSTANCE == null) {
                    INSTANCE = getDefaultPool();
                }
            }
        }
        return INSTANCE;
    }

    @NonNull
    private static ThreadPool getDefaultPool() {
        return new ThreadPool() {
            @Override
            public void executeMain(Runnable r) {
                TaskManager.getInstance().executeMain(r);
            }

            @Override
            public void executeTask(Runnable r) {
                TaskManager.getInstance().executeTask(r);
            }

            @Override
            public void executeNew(Runnable r) {
                TaskManager.getInstance().executeNew(r);
            }
        };
    }

    /**
     * Execute sync task in the main thread
     */
    public abstract void executeMain(Runnable r);

    /**
     * Execute async task in the cached thread pool
     */
    public abstract void executeTask(Runnable r);

    /**
     * Execute async task in a new thread
     */
    public abstract void executeNew(Runnable r);
}
