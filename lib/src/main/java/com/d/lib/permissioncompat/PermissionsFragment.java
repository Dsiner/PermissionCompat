package com.d.lib.permissioncompat;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionsFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST_CODE = 42;

    // Contains all the current permission requests.
    // Once granted or denied, they are removed from it.
    private Map<String, PublishCallback<Permission>> mSubjects = new HashMap<>();
    private boolean mLogging;
    private PermissionCallback<List<Permission>> callback;

    public PermissionsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @TargetApi(Build.VERSION_CODES.M)
    void requestPermissions(@NonNull String[] permissions, PermissionCallback<List<Permission>> callback) {
        this.callback = callback;
        requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return;
        }

        boolean[] shouldShowRequestPermissionRationale = new boolean[permissions.length];

        for (int i = 0; i < permissions.length; i++) {
            shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
        }

        onRequestPermissionsResult(permissions, grantResults, shouldShowRequestPermissionRationale);
    }

    void onRequestPermissionsResult(String permissions[], int[] grantResults, boolean[] shouldShowRequestPermissionRationale) {
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
            boolean granted;
            if (PermissionCompat.isMarshmallow() && "Xiaomi".equalsIgnoreCase(Build.MANUFACTURER)) {
                granted = PermissionCompat.hasSelfPermissionForXiaomi(getActivity(), permissions[i]);
            } else {
                granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
            list.add(new Permission(permissions[i], granted, shouldShowRequestPermissionRationale[i]));
        }
        if (callback != null) {
            callback.onNext(list);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean isGranted(String permission) {
        if (PermissionCompat.isMarshmallow() && "Xiaomi".equalsIgnoreCase(Build.MANUFACTURER)) {
            return PermissionCompat.hasSelfPermissionForXiaomi(getActivity(), permission);
        }
        return getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean isRevoked(String permission) {
        return getActivity().getPackageManager().isPermissionRevokedByPolicy(permission, getActivity().getPackageName());
    }

    public void setLogging(boolean logging) {
        mLogging = logging;
    }

    public PublishCallback<Permission> getSubjectByPermission(@NonNull String permission) {
        return mSubjects.get(permission);
    }

    public boolean containsByPermission(@NonNull String permission) {
        return mSubjects.containsKey(permission);
    }

    public PublishCallback<Permission> setSubjectForPermission(@NonNull String permission, @NonNull PublishCallback<Permission> subject) {
        return mSubjects.put(permission, subject);
    }

    void log(String message) {
        if (mLogging) {
            Log.d(PermissionCompat.TAG, message);
        }
    }
}
