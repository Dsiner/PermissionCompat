package com.d.lib.permissioncompat.support.thread;

/**
 * TaskScheduler
 * Created by D on 2018/5/15.
 */
public class TaskScheduler extends ThreadPool {

    TaskScheduler() {
    }

    /**
     * Execute sync task in the main thread
     */
    @Override
    public void executeMain(Runnable r) {
        TaskManager.getIns().executeMain(r);
    }

    /**
     * Execute async task in the cached thread pool
     */
    @Override
    public void executeTask(Runnable r) {
        TaskManager.getIns().executeTask(r);
    }

    /**
     * Execute async task in a new thread
     */
    @Override
    public void executeNew(Runnable r) {
        TaskManager.getIns().executeNew(r);
    }
}
