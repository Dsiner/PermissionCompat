package com.d.lib.permissioncompat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.AppOpsManagerCompat;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;

import com.d.lib.permissioncompat.support.lollipop.PermissionsChecker;
import com.d.lib.permissioncompat.support.xiaomi.PermissionsFragmentXiaomi;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionXiaomi
 * Created by D on 2018/4/26.
 */
public class PermissionXiaomi extends PermissionCompat {

    PermissionXiaomi(@NonNull Activity activity) {
        super(activity);
    }

    @Override
    protected PermissionsFragment getPermissionsFragment(Activity activity) {
        PermissionsFragment permissionsFragment = findPermissionsFragment(activity);
        boolean isNewInstance = permissionsFragment == null;
        if (isNewInstance) {
            permissionsFragment = new PermissionsFragmentXiaomi();
            FragmentManager fragmentManager = activity.getFragmentManager();
            fragmentManager.beginTransaction()
                    .add(permissionsFragment, TAG)
                    .commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return permissionsFragment;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void requestImplementation(String... permissions) {
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
                                mCallback.onNext(confirmPermission(pers));
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
                        mCallback.onNext(confirmPermission(piss));
                        mCallback.onComplete();
                    }
                });
            }
        }
    }

    private Permission confirmPermission(List<Permission> permissions) {
        if ("Redmi Note 3".equalsIgnoreCase(Build.MODEL)) {
            List<Permission> list = new ArrayList<>();
            for (Permission p : permissions) {
                if (p.granted && !PermissionsChecker.isPermissionGranted(mPermissionsFragment.getActivity(), p.name)) {
                    list.add(p);
                }
            }
            if (list.size() > 0) {
                permissions.removeAll(list);
                for (Permission p : list) {
                    list.add(new Permission(p.name, false, true));
                }
                permissions.addAll(list);
            }
        }
        return new Permission(permissions);
    }

    @Override
    public boolean isGranted(String permission) {
        return hasSelfPermissionForXiaomi(mPermissionsFragment.getActivity(), permission);
    }

    public static boolean hasSelfPermissionForXiaomi(Context context, String permission) {
        if ("Redmi Note 3".equalsIgnoreCase(Build.MODEL)) {
            return hasSelfPermissionForXiaomiOS(context, permission)
                    && PermissionsChecker.isPermissionGranted(context, permission);
        }
        return hasSelfPermissionForXiaomiOS(context, permission);
    }

    private static boolean hasSelfPermissionForXiaomiOS(Context context, String permission) {
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
}
