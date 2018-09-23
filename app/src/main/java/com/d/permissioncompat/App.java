package com.d.permissioncompat;

import android.app.Application;

import com.d.lib.permissioncompat.support.PermissionSupport;
import com.d.lib.permissioncompat.support.threadpool.ThreadPool;
import com.d.lib.taskscheduler.TaskScheduler;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Set support level
        PermissionSupport.setLevel(PermissionSupport.SUPPORT_LEVEL_M_XIAOMI);
        // You can set the thread pool yourself here, otherwise the default will be used.
        PermissionSupport.setThreadPool(new ThreadPool() {
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
