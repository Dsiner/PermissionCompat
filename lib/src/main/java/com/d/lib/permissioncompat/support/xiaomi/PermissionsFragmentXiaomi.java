package com.d.lib.permissioncompat.support.xiaomi;

import com.d.lib.permissioncompat.PermissionXiaomi;
import com.d.lib.permissioncompat.PermissionsFragment;

/**
 * PermissionsFragmentXiaomi
 * Created by D on 2018/4/26.
 */
public class PermissionsFragmentXiaomi extends PermissionsFragment {

    @Override
    protected boolean isGranted(String permission, int grantResult, boolean shouldShowRequestPermissionRationale) {
        return PermissionXiaomi.hasSelfPermissionForXiaomi(mContext, permission);
    }
}
