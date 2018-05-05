# PermissionCompat for Android

[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![API](https://img.shields.io/badge/API-9%2B-green.svg?style=flat)](https://android-arsenal.com/api?level=9)

## Usage
```java
        PermissionCompat.with(activity)
                .requestEachCombined(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribeOn(PermissionSchedulers.io())
                .observeOn(PermissionSchedulers.mainThread())
                .requestPermissions(new PermissionCallback<Permission>() {
                    @Override
                    public void onNext(Permission permission) {
                        if (permission.granted) {
                            // All permissions are granted !
                            Toast.makeText(getApplicationContext(), "All permissions are granted", Toast.LENGTH_SHORT).show();
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            // At least one denied permission without ask never again
                            Toast.makeText(getApplicationContext(), "Permission without ask never again", Toast.LENGTH_SHORT).show();
                        } else {
                            // At least one denied permission with ask never again
                            // Need to go to the settings
                            Toast.makeText(getApplicationContext(), "Need to go to the settings", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
```

More usage see [Demo](app/src/main/java/com/d/permissioncompat/MainActivity.java)

## Licence

```txt
Copyright 2018 D

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
