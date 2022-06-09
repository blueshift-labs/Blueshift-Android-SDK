package com.blueshift;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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
    private static final int mReqCode = 100;
    private static final String TAG = "PermissionsActivity";
    private static final String PERMISSION = "android.permission_name";

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static void launchForPushNotificationPermission(Context context) {
        if (context != null) {
            Intent intent = new Intent(context, BlueshiftPermissionsActivity.class);
            intent.putExtra(PERMISSION, Manifest.permission.POST_NOTIFICATIONS);
            context.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String permission = getIntent().getStringExtra(PERMISSION);
        if (permission != null && permission.length() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestForRuntimePermission(permission);
            } else {
                done();
            }
        } else {
            done();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestForRuntimePermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            BlueshiftLogger.d(TAG, permission + " - Already GRANTED.");
            done();
        } else {
            int count = BlueShiftPreference.getPermissionDenialCount(this, permission);
            if (count > 0 && !shouldShowRequestPermissionRationale(permission)) {
                BlueshiftLogger.d(TAG, permission + " - DENIED twice (Don't ask again).");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    openPushNotificationSettings();
                } else {
                    done();
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, mReqCode);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == mReqCode && grantResults.length > 0) {
            String permission = permissions[0];
            int grantResult = grantResults[0];

            switch (grantResult) {
                case PackageManager.PERMISSION_GRANTED:
                    BlueshiftLogger.d(TAG, permission + " - GRANTED.");
                    break;
                case PackageManager.PERMISSION_DENIED:
                    if (shouldShowRequestPermissionRationale(permission)) {
                        BlueshiftLogger.d(TAG, permission + " - DENIED once.");
                        BlueShiftPreference.incrementPermissionDenialCount(this, permission);
                    }
                    break;
                default:
                    BlueshiftLogger.e(TAG, "Unhandled grant result: " + grantResult);
            }
        }

        done();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openPushNotificationSettings() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bsft_push_permission_popup_title)
                .setMessage(R.string.bsft_push_permission_popup_message)
                .setNegativeButton(
                        R.string.bsft_push_permission_popup_ignore,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                done();
                            }
                        })
                .setPositiveButton(
                        R.string.bsft_push_permission_popup_settings,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                                startActivity(intent);

                                dialogInterface.dismiss();
                                done();
                            }
                        })
                .show();
    }

    private void done() {
        if (!isFinishing()) finish();
    }
}
