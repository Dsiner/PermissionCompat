package com.d.lib.permissioncompat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionCompat
 * Created by D on 2018/4/13.
 */
public class PermissionCompat {

    static final String TAG = "PermissionCompat";

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

    public boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
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
                            }
                        });
                    }
                }
            });
        } else {
            if (mCallback != null) {
                switchThread(observeOnScheduler, new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onNext(new Permission(piss));
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

    boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    void onRequestPermissionsResult(String permissions[], int[] grantResults) {
        mPermissionsFragment.onRequestPermissionsResult(permissions, grantResults, new boolean[permissions.length]);
    }

    /**
     * Print thread information where the current code is
     */
    public static void printThread(String tag) {
        Log.d(TAG, tag + " " + Thread.currentThread().getId() + "--NAME--" + Thread.currentThread().getName());
    }
}
