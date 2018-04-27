package com.d.lib.permissioncompat.support.xiaomi;

import com.d.lib.permissioncompat.PermissionXiaomi;
import com.d.lib.permissioncompat.PermissionsFragment;
import com.d.lib.permissioncompat.support.ManufacturerSupport;
import com.d.lib.permissioncompat.support.lollipop.PermissionsChecker;

/**
 * PermissionsFragmentXiaomi
 * Created by D on 2018/4/26.
 */
public class PermissionsFragmentXiaomi extends PermissionsFragment {

    @Override
    protected boolean isGranted(String permission, int grantResult, boolean shouldShowRequestPermissionRationale) {
        if (ManufacturerSupport.isXiaomiSpecial()) {
            return isFinish() || PermissionXiaomi.hasSelfPermissionForXiaomiOS(mContext, permission)
                    && PermissionsChecker.requestPermissions(mContext, permission);
        }
        return PermissionXiaomi.hasSelfPermissionForXiaomiOS(mContext, permission);
    }
}
