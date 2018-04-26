package com.d.lib.permissioncompat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.d.lib.permissioncompat.support.lollipop.PermissionsChecker;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionLollipop
 * Created by D on 2018/4/26.
 */
public class PermissionLollipop extends PermissionCompat {

    PermissionLollipop(@NonNull Activity activity) {
        super(activity);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void requestImplementation(final String... permissions) {
        final List<Permission> piss = new ArrayList<>();
        final List<PublishCallback<Permission>> publishs = new ArrayList<>(permissions.length);
        final List<String> unrequestedPermissions = new ArrayList<>();

        // In case of multiple permissions, we create an Observable for each of them.
        // At the end, the observables are combined to have a unique response.
        for (String permission : permissions) {
            mPermissionsFragment.log("Requesting permission " + permission);
            if (isGranted(permission)) {
                Permission p = new Permission(permission, true, false);
                piss.add(p);
                publishs.add(PublishCallback.create(p));
                continue;
            }
            piss.add(new Permission(permission, false, true));
            unrequestedPermissions.add(permission);
            PublishCallback<Permission> subject = PublishCallback.create();
            publishs.add(subject);
        }
        if (!unrequestedPermissions.isEmpty()) {
            String[] unrequestedPermissionsArray = unrequestedPermissions.toArray(new String[unrequestedPermissions.size()]);
            mPermissionsFragment.log("deny permissions " + TextUtils.join(", ", unrequestedPermissionsArray));
        }
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

    @Override
    public boolean isGranted(String permission) {
        return PermissionsChecker.isPermissionGranted(mPermissionsFragment.getActivity(), permission);
    }
}
