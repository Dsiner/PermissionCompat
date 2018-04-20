package com.d.lib.permissioncompat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.AppOpsManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionCompat
 * For more detail, check this https://github.com/tbruyelle/RxPermissions
 * Created by D on 2018/4/13.
 */
public class PermissionCompat {

    static final String TAG = "PermissionCompat";

    // Map of dangerous permissions introduced in later framework versions.
    // Used to conditionally bypass permission-hold checks on older devices.
    private static final SimpleArrayMap<String, Integer> MIN_SDK_PERMISSIONS;

    static {
        MIN_SDK_PERMISSIONS = new SimpleArrayMap<String, Integer>(8);
        MIN_SDK_PERMISSIONS.put("com.android.voicemail.permission.ADD_VOICEMAIL", 14);
        MIN_SDK_PERMISSIONS.put("android.permission.BODY_SENSORS", 20);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_CALL_LOG", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.READ_EXTERNAL_STORAGE", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.USE_SIP", 9);
        MIN_SDK_PERMISSIONS.put("android.permission.WRITE_CALL_LOG", 16);
        MIN_SDK_PERMISSIONS.put("android.permission.SYSTEM_ALERT_WINDOW", 23);
        MIN_SDK_PERMISSIONS.put("android.permission.WRITE_SETTINGS", 23);
    }

    String[] mPermissions;
    PermissionCallback<Permission> mCallback;
    PermissionsFragment mPermissionsFragment;
    PermissionSchedulers.Schedulers subscribeScheduler = PermissionSchedulers.Schedulers.DEFAULT_THREAD;
    PermissionSchedulers.Schedulers observeOnScheduler = PermissionSchedulers.Schedulers.DEFAULT_THREAD;

    public PermissionCompat(@NonNull Activity activity) {
        mPermissionsFragment = getPermissionsFragment(activity);
    }

    private PermissionsFragment getPermissionsFragment(Activity activity) {
        PermissionsFragment permissionsFragment = findPermissionsFragment(activity);
        boolean isNewInstance = permissionsFragment == null;
        if (isNewInstance) {
            permissionsFragment = new PermissionsFragment();
            FragmentManager fragmentManager = activity.getFragmentManager();
            fragmentManager.beginTransaction()
                    .add(permissionsFragment, TAG)
                    .commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return permissionsFragment;
    }

    private PermissionsFragment findPermissionsFragment(Activity activity) {
        return (PermissionsFragment) activity.getFragmentManager().findFragmentByTag(TAG);
    }

    public void setLogging(boolean logging) {
        mPermissionsFragment.setLogging(logging);
    }

    /**
     * Map emitted items from the source observable into one combined {@link Permission} object. Only if all permissions are granted,
     * permission also will be granted. If any permission has {@code shouldShowRationale} checked, than result also has it checked.
     * <p>
     * If one or several permissions have never been requested, invoke the related framework method
     * to ask the user if he allows the permissions.
     */
    public PermissionCompat requestEachCombined(final String... permissions) {
        this.mPermissions = permissions;
        return this;
    }

    public PermissionCompat subscribeOn(PermissionSchedulers.Schedulers scheduler) {
        this.subscribeScheduler = scheduler;
        return this;
    }

    public PermissionCompat observeOn(PermissionSchedulers.Schedulers scheduler) {
        this.observeOnScheduler = scheduler;
        return this;
    }

    public void requestPermissions(PermissionCallback<Permission> callback) {
        if (mPermissions == null || mPermissions.length == 0) {
            throw new IllegalArgumentException("PermissionCompat.request/requestEach requires at least one input permission");
        }
        this.mCallback = callback;
        switchThread(subscribeScheduler, new Runnable() {
            @Override
            public void run() {
                requestImplementation(mPermissions);
            }
        });
    }

    private void switchThread(PermissionSchedulers.Schedulers scheduler, final Runnable runnable) {
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

    @TargetApi(Build.VERSION_CODES.M)
    private void requestImplementation(final String... permissions) {
        final List<Permission> piss = new ArrayList<>();
        final List<PublishCallback<Permission>> publishs = new ArrayList<>(permissions.length);
        final List<String> unrequestedPermissions = new ArrayList<>();

        // In case of multiple permissions, we create an Observable for each of them.
        // At the end, the observables are combined to have a unique response.
        for (String permission : permissions) {
            mPermissionsFragment.log("Requesting permission " + permission);
            if (isGranted(permission)) {
                // Already granted, or not Android M
                // Return a granted Permission object.
                Permission p = new Permission(permission, true, false);
                piss.add(p);
                publishs.add(PublishCallback.create(p));
                continue;
            }

            if (isRevoked(permission)) {
                // Revoked by a policy, return a denied Permission object.
                Permission p = new Permission(permission, false, false);
                piss.add(p);
                publishs.add(PublishCallback.create(p));
                continue;
            }

            PublishCallback<Permission> subject = mPermissionsFragment.getSubjectByPermission(permission);
            // Create a new subject if not exists
            if (subject == null) {
                unrequestedPermissions.add(permission);
                subject = PublishCallback.create();
                mPermissionsFragment.setSubjectForPermission(permission, subject);
            }

            publishs.add(subject);
        }

        if (!unrequestedPermissions.isEmpty()) {
            String[] unrequestedPermissionsArray = unrequestedPermissions.toArray(new String[unrequestedPermissions.size()]);
            mPermissionsFragment.log("requestPermissionsFromFragment " + TextUtils.join(", ", unrequestedPermissionsArray));
            mPermissionsFragment.requestPermissions(unrequestedPermissionsArray, new PermissionCallback<List<Permission>>() {
                @Override
                public void onNext(List<Permission> permission) {
                    final List<Permission> pers = new ArrayList<>();
                    pers.addAll(piss);
                    pers.addAll(permission);
                    if (mCallback != null) {
                        switchThread(observeOnScheduler, new Runnable() {
                            @Override
                            public void run() {
                                mCallback.onNext(new Permission(pers));
                                mCallback.onComplete();
                            }
                        });
                    }
                }

                @Override
                public void onError(Throwable e) {
                    mCallback.onError(e);
                    mCallback.onComplete();
                }
            });
        } else {
            if (mCallback != null) {
                switchThread(observeOnScheduler, new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onNext(new Permission(piss));
                        mCallback.onComplete();
                    }
                });
            }
        }
    }

    /**
     * Invokes Activity.shouldShowRequestPermissionRationale and wraps
     * the returned value in an observable.
     * <p>
     * In case of multiple permissions, only emits true if
     * Activity.shouldShowRequestPermissionRationale returned true for
     * all revoked permissions.
     * <p>
     * You shouldn't call this method if all permissions have been granted.
     * <p>
     * For SDK &lt; 23, the observable will always emit false.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean shouldShowRequestPermissionRationale(final Activity activity, final String... permissions) {
        if (!isMarshmallow()) {
            return false;
        }
        return shouldShowRequestPermissionRationaleImplementation(activity, permissions);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean shouldShowRequestPermissionRationaleImplementation(final Activity activity, final String... permissions) {
        for (String p : permissions) {
            if (!isGranted(p) && !activity.shouldShowRequestPermissionRationale(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the permission is already granted.
     * <p>
     * Always true if SDK &lt; 23.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isGranted(String permission) {
        return !isMarshmallow() || mPermissionsFragment.isGranted(permission);
    }

    /**
     * Returns true if the permission has been revoked by a policy.
     * <p>
     * Always false if SDK &lt; 23.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isRevoked(String permission) {
        return isMarshmallow() && mPermissionsFragment.isRevoked(permission);
    }

    static boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Checks all given permissions have been granted.
     *
     * @param grantResults results
     * @return returns true if all permissions have been granted.
     */
    public static boolean verifyPermissions(int... grantResults) {
        if (grantResults.length == 0) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the permissions are all already granted.
     * <p>
     * Always true if SDK &lt; 23.
     */
    public static boolean hasSelfPermissions(@NonNull Context context, String... permissions) {
        if (permissions == null || permissions.length <= 0) {
            throw new IllegalArgumentException("permissions is null or empty");
        }
        if (isMarshmallow()) {
            for (String permission : permissions) {
                if (permissionExists(permission) && !hasSelfPermission(context, permission)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if the permission exists in this SDK version
     *
     * @param permission permission
     * @return returns true if the permission exists in this SDK version
     */
    private static boolean permissionExists(String permission) {
        // Check if the permission could potentially be missing on this device
        Integer minVersion = MIN_SDK_PERMISSIONS.get(permission);
        // If null was returned from the above call, there is no need for a device API level check for the permission;
        // otherwise, we check if its minimum API level requirement is met
        return minVersion == null || Build.VERSION.SDK_INT >= minVersion;
    }

    /**
     * Determine context has access to the given permission.
     * <p>
     * This is a workaround for RuntimeException of Parcel#readException.
     * For more detail, check this issue https://github.com/hotchemi/PermissionsDispatcher/issues/107
     *
     * @param context    context
     * @param permission permission
     * @return returns true if context has access to the given permission, false otherwise.
     * @see #hasSelfPermissions(Context, String...)
     */
    private static boolean hasSelfPermission(Context context, String permission) {
        if (isMarshmallow()) {
            if ("Xiaomi".equalsIgnoreCase(Build.MANUFACTURER)) {
                return hasSelfPermissionForXiaomi(context, permission);
            }
            try {
                return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            } catch (RuntimeException t) {
                return false;
            }
        }
        return true;
    }

    static boolean hasSelfPermissionForXiaomi(Context context, String permission) {
        String permissionToOp = AppOpsManagerCompat.permissionToOp(permission);
        if (permissionToOp == null) {
            // in case of normal permissions(e.g. INTERNET)
            return true;
        }
        int noteOp = AppOpsManagerCompat.noteOp(context, permissionToOp, Process.myUid(), context.getPackageName());
        try {
            return noteOp == AppOpsManagerCompat.MODE_ALLOWED
                    && PermissionChecker.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        } catch (RuntimeException t) {
            return false;
        }
    }

    private boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    /**
     * Print thread information where the current code is
     */
    public static void printThread(String tag) {
        Log.d(TAG, tag + " " + Thread.currentThread().getId() + "--NAME--" + Thread.currentThread().getName());
    }
}
