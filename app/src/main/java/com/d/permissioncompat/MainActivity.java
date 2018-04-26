package com.d.permissioncompat;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.d.lib.permissioncompat.Permission;
import com.d.lib.permissioncompat.PermissionCallback;
import com.d.lib.permissioncompat.PermissionCompat;
import com.d.lib.permissioncompat.PermissionSchedulers;

public class MainActivity extends AppCompatActivity {

    private static String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                .requestPermissions(new PermissionCallback<Permission>() {
                    @Override
                    public void onNext(Permission permission) {
                        if (permission.granted) {
                            // All permissions are granted !
                            Toast.makeText(MainActivity.this, "All permissions are granted", Toast.LENGTH_SHORT).show();
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            // At least one denied permission without ask never again
                            Toast.makeText(MainActivity.this, "Permission without ask never again", Toast.LENGTH_SHORT).show();
                        } else {
                            // At least one denied permission with ask never again
                            // Need to go to the settings
                            Toast.makeText(MainActivity.this, "Need to go to the settings", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
