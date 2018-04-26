package com.d.lib.permissioncompat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;

import com.d.lib.permissioncompat.support.ManufacturerSupport;
import com.d.lib.permissioncompat.support.PermissionSupport;
import com.d.lib.permissioncompat.support.lollipop.PermissionsChecker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * PermissionCompat
 * For more detail, check this https://github.com/tbruyelle/RxPermissions
 * Created by D on 2018/4/13.
 */
public class PermissionCompat {

    public final static String TAG = "PermissionCompat";

    // Map of dangerous permissions introduced in later framework versions.
    // Used to conditionally bypass permission-hold checks on older devices.
    protected final static SimpleArrayMap<String, Integer> MIN_SDK_PERMISSIONS;

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

    protected Context mContext;
    protected WeakReference<Activity> mRefActivity;
    protected String[] mPermissions;
    protected WeakReference<PermissionCallback<Permission>> mCallback;
    protected PermissionsFragment mPermissionsFragment;
    protected PermissionSchedulers.Schedulers subscribeScheduler = PermissionSchedulers.Schedulers.DEFAULT_THREAD;
    protected PermissionSchedulers.Schedulers observeOnScheduler = PermissionSchedulers.Schedulers.DEFAULT_THREAD;

    PermissionCompat(@NonNull Activity activity) {
        mContext = activity.getApplicationContext();
        mRefActivity = new WeakReference<>(activity);
        mPermissionsFragment = getPermissionsFragment(activity);
    }

    protected Activity getActivity() {
        return mRefActivity != null ? mRefActivity.get() : null;
    }

    protected PermissionCallback<Permission> getCallback() {
        return mCallback != null ? mCallback.get() : null;
    }

    protected boolean isFinish() {
        return getActivity() == null || getActivity().isFinishing() || getCallback() == null;
    }

    protected PermissionsFragment getPermissionsFragment(Activity activity) {
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

    protected PermissionsFragment findPermissionsFragment(Activity activity) {
        return (PermissionsFragment) activity.getFragmentManager().findFragmentByTag(TAG);
    }

    public static PermissionCompat with(Activity activity) {
        int type = PermissionSupport.getType();
        if (type == PermissionSupport.TYPE_LOLLIPOP) {
            return new PermissionLollipop(activity);
        } else if (type == PermissionSupport.TYPE_MARSHMALLOW_XIAOMI) {
            return new PermissionXiaomi(activity);
        }
        return new PermissionCompat(activity);
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
        this.mCallback = new WeakReference<>(callback);
        switchThread(subscribeScheduler, new Runnable() {
            @Override
            public void run() {
                if (isFinish()) {
                    return;
                }
                requestImplementation(mPermissions);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected void requestImplementation(final String... permissions) {
        final List<Permission> ps = new ArrayList<>();
        final List<PublishCallback<Permission>> publishs = new ArrayList<>(permissions.length);
        final List<String> unrequestedPermissions = new ArrayList<>();

        // In case of multiple permissions, we create an Observable for each of them.
        // At the end, the observables are combined to have a unique response.
        for (String permission : permissions) {
            Log.d(PermissionCompat.TAG, "Requesting permission " + permission);
            if (isGranted(permission)) {
                // Already granted, or not Android M
                // Return a granted Permission object.
                Permission p = new Permission(permission, true, false);
                ps.add(p);
                publishs.add(PublishCallback.create(p));
                continue;
            }

            if (isRevoked(permission)) {
                // Revoked by a policy, return a denied Permission object.
                Permission p = new Permission(permission, false, false);
                ps.add(p);
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
            Log.d(PermissionCompat.TAG, "requestPermissionsFromFragment " + TextUtils.join(", ", unrequestedPermissionsArray));
            if (isFinish()) {
                return;
            }
            mPermissionsFragment.requestPermissions(unrequestedPermissionsArray, new PermissionCallback<List<Permission>>() {
                @Override
                public void onNext(List<Permission> permission) {
                    if (isFinish()) {
                        return;
                    }
                    List<Permission> list = new ArrayList<>();
                    list.addAll(ps);
                    list.addAll(permission);
                    final Permission result = combinePermission(list);
                    switchThread(observeOnScheduler, new Runnable() {
                        @Override
                        public void run() {
                            if (isFinish()) {
                                return;
                            }
                            getCallback().onNext(result);
                            getCallback().onComplete();
                        }
                    });
                }

                @Override
                public void onError(Throwable e) {
                    if (isFinish()) {
                        return;
                    }
                    getCallback().onError(e);
                    getCallback().onComplete();
                }
            });
        } else {
            final Permission result = combinePermission(ps);
            switchThread(observeOnScheduler, new Runnable() {
                @Override
                public void run() {
                    if (isFinish()) {
                        return;
                    }
                    getCallback().onNext(result);
                    getCallback().onComplete();
                }
            });
        }
    }

    protected Permission combinePermission(List<Permission> permissions) {
        return new Permission(permissions);
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
        if (!ManufacturerSupport.isMarshmallow()) {
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
        return !ManufacturerSupport.isMarshmallow()
                || mContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns true if the permission has been revoked by a policy.
     * <p>
     * Always false if SDK &lt; 23.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isRevoked(String permission) {
        return ManufacturerSupport.isMarshmallow()
                && mContext.getPackageManager().isPermissionRevokedByPolicy(permission, mContext.getPackageName());
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
     */
    public static boolean hasSelfPermissions(@NonNull Context context, String... permissions) {
        context = context.getApplicationContext();
        if (permissions == null || permissions.length <= 0) {
            throw new IllegalArgumentException("permissions is null or empty");
        }
        for (String permission : permissions) {
            if (permissionExists(permission) && !hasSelfPermission(context, permission)) {
                return false;
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
        context = context.getApplicationContext();
        int type = PermissionSupport.getType();
        if (type == PermissionSupport.TYPE_LOLLIPOP) {
            return PermissionsChecker.isPermissionGranted(context, permission);
        } else if (type == PermissionSupport.TYPE_MARSHMALLOW_XIAOMI) {
            return PermissionXiaomi.hasSelfPermissionForXiaomi(context, permission);
        } else if (type == PermissionSupport.TYPE_MARSHMALLOW) {
            try {
                return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            } catch (RuntimeException t) {
                return false;
            }
        }
        return true;
    }

    protected void switchThread(PermissionSchedulers.Schedulers scheduler, final Runnable runnable) {
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
