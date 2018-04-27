package com.d.lib.permissioncompat;

import android.os.Handler;
import android.os.Looper;

public class PermissionSchedulers {

    enum Schedulers {
        DEFAULT_THREAD, IO, MAIN_THREAD
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
    public static void switchThread(PermissionSchedulers.Schedulers scheduler, final Runnable runnable) {
        if (scheduler == PermissionSchedulers.Schedulers.IO) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            }).start();
            return;
        } else if (scheduler == PermissionSchedulers.Schedulers.MAIN_THREAD) {
            if (!isMainThread()) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                });
                return;
            }
        }
        if (runnable != null) {
            runnable.run();
        }
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
