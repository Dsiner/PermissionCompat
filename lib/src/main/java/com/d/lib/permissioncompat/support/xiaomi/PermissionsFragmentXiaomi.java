package com.d.lib.permissioncompat.support.xiaomi;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.d.lib.permissioncompat.Permission;
import com.d.lib.permissioncompat.PermissionCompat;
import com.d.lib.permissioncompat.PermissionXiaomi;
import com.d.lib.permissioncompat.PermissionsFragment;
import com.d.lib.permissioncompat.PublishCallback;
import com.d.lib.permissioncompat.support.ManufacturerSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionsFragmentXiaomi
 * Created by D on 2018/4/26.
 */
public class PermissionsFragmentXiaomi extends PermissionsFragment {

    @Override
    protected void onRequestPermissionsResult(String[] permissions, int[] grantResults, boolean[] shouldShowRequestPermissionRationale) {
        List<Permission> list = new ArrayList<Permission>();
        for (int i = 0, size = permissions.length; i < size; i++) {
            log("onRequestPermissionsResult  " + permissions[i]);
            // Find the corresponding subject
            PublishCallback<Permission> subject = mSubjects.get(permissions[i]);
            if (subject == null) {
                // No subject found
                Log.e(PermissionCompat.TAG, "PermissionCompat.onRequestPermissionsResult invoked but didn't find the corresponding permission request.");
                if (callback != null) {
                    callback.onError(new Exception("PermissionCompat.onRequestPermissionsResult invoked but didn't find the corresponding permission request."));
                }
                return;
            }
            mSubjects.remove(permissions[i]);
            boolean granted = PermissionXiaomi.hasSelfPermissionForXiaomi(getActivity(), permissions[i]);
            list.add(new Permission(permissions[i], granted, shouldShowRequestPermissionRationale[i]));
        }
        if (callback != null) {
            callback.onNext(list);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected boolean isGranted(String permission) {
        if (ManufacturerSupport.isMarshmallow() && ManufacturerSupport.isXiaomi()) {
            return PermissionXiaomi.hasSelfPermissionForXiaomi(getActivity(), permission);
        }
        return getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
}
