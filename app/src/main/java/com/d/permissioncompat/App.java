package com.d.permissioncompat;

import android.app.Application;

import com.d.lib.permissioncompat.support.PermissionSupport;
import com.d.lib.permissioncompat.support.thread.ThreadPool;
import com.d.lib.taskscheduler.TaskScheduler;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initPermissionCompat();
    }

    private void initPermissionCompat() {
        PermissionSupport.setLevel(PermissionSupport.SUPPORT_LEVEL_M_XIAOMI);
        PermissionSupport.setPool(new ThreadPool() {
            @Override
            public void executeMain(Runnable r) {
                TaskScheduler.executeMain(r);
            }

            @Override
            public void executeTask(Runnable r) {
                TaskScheduler.executeTask(r);
            }

            @Override
            public void executeNew(Runnable r) {
                TaskScheduler.executeNew(r);
            }
        });
    }
}
