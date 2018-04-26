package com.d.lib.permissioncompat.support;

/**
 * PermissionSupport
 * Created by D on 2018/4/26.
 */
public class PermissionSupport {
    public final static int TYPE_MARSHMALLOW = 0;
    public final static int TYPE_MARSHMALLOW_XIAOMI = 1;
    public final static int TYPE_LOLLIPOP = 2;
    public final static int TYPE_UNSUPPORT = 3;

    public static int getType() {
        if (ManufacturerSupport.isUnderMNeedChecked()) {
            return TYPE_LOLLIPOP;
        } else if (ManufacturerSupport.isMarshmallow() && ManufacturerSupport.isXiaomi()) {
            return TYPE_MARSHMALLOW_XIAOMI;
        } else if (ManufacturerSupport.isMarshmallow()) {
            return TYPE_MARSHMALLOW;
        }
        return TYPE_UNSUPPORT;
    }
}
