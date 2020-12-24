package com.d.lib.permissioncompat;

import android.os.Looper;
import android.support.annotation.NonNull;

import com.d.lib.permissioncompat.support.threadpool.ThreadPool;

public class PermissionSchedulers {

    public static Schedulers defaultThread() {
        return Schedulers.DEFAULT_THREAD;
    }

    public static Schedulers newThread() {
        return Schedulers.NEW_THREAD;
    }

    public static Schedulers io() {
        return Schedulers.IO;
    }

    public static Schedulers mainThread() {
        return Schedulers.MAIN_THREAD;
    }

    /**
     * Switch thread
     */
    public static void switchThread(PermissionSchedulers.Schedulers scheduler, @NonNull final Runnable runnable) {
        if (scheduler == PermissionSchedulers.Schedulers.NEW_THREAD) {
            ThreadPool.getInstance().executeNew(runnable);
            return;
        } else if (scheduler == PermissionSchedulers.Schedulers.IO) {
            ThreadPool.getInstance().executeTask(runnable);
            return;
        } else if (scheduler == PermissionSchedulers.Schedulers.MAIN_THREAD) {
            ThreadPool.getInstance().executeMain(runnable);
            return;
        }
        runnable.run();
    }

    public static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    enum Schedulers {
        DEFAULT_THREAD, NEW_THREAD, IO, MAIN_THREAD
    }
}
