package com.d.lib.permissioncompat;

/**
 * PermissionCallback
 * Created by D on 2017/10/24.
 */
public abstract class PermissionCallback<R> {

    public abstract void onNext(R permission);

    public void onError(Throwable e) {
    }

    public void onComplete() {
    }
}