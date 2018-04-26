package com.d.lib.permissioncompat;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.AppOpsManagerCompat;
import android.support.v4.content.PermissionChecker;

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

    @Override
    protected Permission combinePermission(List<Permission> permissions) {
        if ("Redmi Note 3".equalsIgnoreCase(Build.MODEL)) {
            List<Permission> list = new ArrayList<>();
            for (Permission p : permissions) {
                if (p.granted && !PermissionsChecker.isPermissionGranted(mContext, p.name)) {
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
        return hasSelfPermissionForXiaomi(mContext, permission);
    }

    public static boolean hasSelfPermissionForXiaomi(Context context, String permission) {
        context = context.getApplicationContext();
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
