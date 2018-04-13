package com.d.lib.permissioncompat;

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
}
