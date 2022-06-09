package com.blueshift;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BlueshiftPermissionsActivity extends Activity {
    private static final String TAG = "PermissionsActivity";
    public static final int reqCode = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPushNotificationPermission(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            finishAndCleanup();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == reqCode && grantResults.length > 0) {
            for (int index = 0; index < permissions.length; index++) {
                String permission = permissions[index];
                int grantResult = grantResults[index];

                switch (grantResult) {
                    case PackageManager.PERMISSION_GRANTED:
                        BlueshiftLogger.d(TAG, permission + " - GRANTED.");
                        break;
                    case PackageManager.PERMISSION_DENIED:
                        BlueshiftLogger.d(TAG, permission + " - DENIED.");
                        if (shouldShowRequestPermissionRationale(permission)) {
                            // permission was denied once
                            BlueShiftPreference.incrementPermissionDenialCount(this, permission);
                        }
                        break;
                    default:
                        BlueshiftLogger.e(TAG, "Unhandled grant result: " + grantResult);
                }
            }
        }

        finishAndCleanup();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPushNotificationPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            BlueshiftLogger.d(TAG, "Permission granted already.");
            finishAndCleanup();
        } else {
            int count = BlueShiftPreference.getPermissionDenialCount(this, permission);
            if (count > 0 && !shouldShowRequestPermissionRationale(permission)) {
                // permission denied twice
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    openPushNotificationSettings();
                } else {
                    finishAndCleanup();
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, reqCode);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openPushNotificationSettings() {
        new AlertDialog.Builder(this)
                .setTitle("PN Permission")
                .setMessage("PN Permission was denied twice. Go to settings to enable it.")
                .setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finishAndCleanup();
                    }
                })
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(intent);

                        dialogInterface.dismiss();
                        finishAndCleanup();
                    }
                })
                .show();
    }

    private void finishAndCleanup() {
        if (!isFinishing()) finish();
    }
}
