package com.d.lib.permissioncompat.support;

import android.support.annotation.IntDef;

import com.d.lib.permissioncompat.support.threadpool.ThreadPool;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * PermissionSupport
 * Created by D on 2018/4/26.
 */
public class PermissionSupport {
    /**
     * Device type
     */
    public static final int TYPE_MARSHMALLOW = 0;
    public static final int TYPE_MARSHMALLOW_XIAOMI = 1;
    public static final int TYPE_LOLLIPOP = 2;
    public static final int TYPE_UNSUPPORT = 3;

    /**
     * Support level
     */
    public static final int SUPPORT_LEVEL_M = 0;
    public static final int SUPPORT_LEVEL_M_XIAOMI = 1;
    public static final int SUPPORT_LEVEL_L = 2;

    /**
     * The default level is SUPPORT_LEVEL_M
     */
    private static int sSupportLevel = SUPPORT_LEVEL_M;

    @IntDef({SUPPORT_LEVEL_M, SUPPORT_LEVEL_M_XIAOMI, SUPPORT_LEVEL_L})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportLevel {
    }

    /**
     * Set support level
     */
    public static void setLevel(@SupportLevel int level) {
        sSupportLevel = level;
    }

    /**
     * Replace thread pool implementation
     */
    public static void setThreadPool(ThreadPool pool) {
        ThreadPool.setThreadPool(pool);
    }

    public static int getType() {
        int type = TYPE_MARSHMALLOW;
        if (ManufacturerSupport.isUnderMNeedChecked()) {
            type = sSupportLevel >= SUPPORT_LEVEL_L ? TYPE_LOLLIPOP : TYPE_MARSHMALLOW;
        } else if (ManufacturerSupport.isMarshmallow() && ManufacturerSupport.isXiaomi()) {
            type = sSupportLevel >= SUPPORT_LEVEL_M_XIAOMI ? TYPE_MARSHMALLOW_XIAOMI : TYPE_MARSHMALLOW;
        } else if (ManufacturerSupport.isMarshmallow()) {
            type = TYPE_MARSHMALLOW;
        }
        return type;
    }

    /**
     * Whether the device is LOLLIPOP or special
     */
    public static boolean isL() {
        int type = getType();
        if (type == TYPE_LOLLIPOP) {
            return true;
        } else if (type == TYPE_MARSHMALLOW_XIAOMI && ManufacturerSupport.isXiaomiSpecial()) {
            return true;
        }
        return false;
    }
}
