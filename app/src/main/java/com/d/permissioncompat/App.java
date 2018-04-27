package com.d.permissioncompat;

import android.app.Application;

import com.d.lib.permissioncompat.support.PermissionSupport;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PermissionSupport.setLevel(PermissionSupport.SUPPORT_LEVEL_L);
    }
}
