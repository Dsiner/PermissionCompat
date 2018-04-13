package com.d.lib.permissioncompat;

/**
 * PublishCallback
 * Created by D on 2017/10/24.
 */
public class PublishCallback<R> extends PermissionCallback<R> {
    private Permission permission;

    PublishCallback() {
    }

    private PublishCallback(Permission permission) {
        this.permission = permission;
    }

    public static <T> PublishCallback<T> create() {
        return new PublishCallback();
    }

    public static PublishCallback<Permission> create(Permission permission) {
        return new PublishCallback(permission);
    }

    @Override
    public void onNext(R permission) {

    }
}