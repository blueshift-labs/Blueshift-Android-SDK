package com.blueshift;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BlueshiftPermissionsActivity extends Activity {
    private static final String TAG = "PermissionsActivity";
    public static final int reqCode = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPushNotificationPermission();
        } else {
            finishAndCleanup();
        }
    }

    @RequiresApi(api = 33)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == reqCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                BlueshiftLogger.d(TAG, "POST_NOTIFICATION permission granted.");
            } else {
                BlueshiftLogger.d(TAG, "POST_NOTIFICATION permission denied.");
                BlueShiftPreference.incrementPermissionDenialCount(
                        this, Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        finishAndCleanup();
    }

    @RequiresApi(api = 33)
    private void requestPushNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            BlueshiftLogger.d(TAG, "POST_NOTIFICATION permission granted.");
            finishAndCleanup();
        } else {
            if (isPermissionDeniedTwice()) {
                openPushNotificationSettings();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        reqCode
                );
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openPushNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);

        finishAndCleanup();
    }

    @RequiresApi(api = 33)
    private boolean isPermissionDeniedTwice() {
        return BlueShiftPreference.getPermissionDenialCount(
                this, Manifest.permission.POST_NOTIFICATIONS) >= 2;
    }

    private void finishAndCleanup() {
        if (!isFinishing()) finish();
    }
}
