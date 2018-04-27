package com.d.permissioncompat;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.d.lib.permissioncompat.Permission;
import com.d.lib.permissioncompat.PermissionCompat;
import com.d.lib.permissioncompat.PermissionSchedulers;
import com.d.lib.permissioncompat.callback.WeakRefPermissionCallback;
import com.d.lib.permissioncompat.callback.WeakRefPermissionSimpleCallback;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    private static String[] PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,

            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.USE_SIP,
            Manifest.permission.PROCESS_OUTGOING_CALLS,

            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,

            Manifest.permission.BODY_SENSORS,
            Manifest.permission.CAMERA,

            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_SMS,
    };

    static class WeakRefCallback extends WeakRefPermissionCallback<MainActivity> {

        public WeakRefCallback(MainActivity view) {
            super(view);
        }

        @Override
        public void onNext(Permission permission) {
            if (isFinish()) {
                Log.v("PermissionCompat", "dsiner permission onNext isFinish");
                return;
            }
            Log.v("PermissionCompat", "dsiner permission onNext");
            if (permission.granted) {
                // All permissions are granted !
                Toast.makeText(getView().getApplicationContext(), "All permissions are granted", Toast.LENGTH_SHORT).show();
            } else if (permission.shouldShowRequestPermissionRationale) {
                // At least one denied permission without ask never again
                Toast.makeText(getView().getApplicationContext(), "Permission without ask never again", Toast.LENGTH_SHORT).show();
            } else {
                // At least one denied permission with ask never again
                // Need to go to the settings
                Toast.makeText(getView().getApplicationContext(), "Need to go to the settings", Toast.LENGTH_SHORT).show();
            }
        }
    }

    static class WeakRefSimpleCallback extends WeakRefPermissionSimpleCallback<MainActivity> {

        public WeakRefSimpleCallback(MainActivity view) {
            super(view);
        }

        @Override
        public void onGranted() {
            if (isFinish()) {
                return;
            }
            // All permissions are granted !
            Toast.makeText(getView().getApplicationContext(), "All permissions are granted", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDeny() {
            if (isFinish()) {
                return;
            }
            // At least one denied permission
            // Need to go to the settings
            Toast.makeText(getView().getApplicationContext(), "Need to go to the settings", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionCompat.checkSelfPermissions(this, new WeakRefSimpleCallback(this), PERMISSIONS);
        findViewById(R.id.btn_permission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions();
            }
        });
    }

    private void requestPermissions() {
        PermissionCompat.with(this).requestEachCombined(PERMISSIONS)
                .subscribeOn(PermissionSchedulers.io())
                .observeOn(PermissionSchedulers.mainThread())
                .requestPermissions(new WeakRefCallback(this));
    }

    private void test() {
        WeakRefCallback callback = new WeakRefCallback(this);
        callback.onNext(new Permission("", true, false));
        Class clazz = callback.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Log.v("PermissionCompat", "dsiner " + field.getName());
            try {
                Log.v("PermissionCompat", "dsiner " + field.get(callback));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
