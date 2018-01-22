package com.blueshift.pn;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.blueshift.Blueshift;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.RichPushConstants;

import java.util.List;


/**
 * @author Rahul Raveendran V P
 *         Created on 22/12/17 @ 3:25 PM
 *         https://github.com/rahulrvp
 */

public class BlueshiftNotificationEventsActivity extends AppCompatActivity {

    private static final String LOG_TAG = "NotificationClick";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            Bundle extraBundle = getIntent().getExtras();
            String action = getIntent().getAction();

            processAction(action, extraBundle);
        }

        // close activity once action is taken.
        finish();
    }

    private void processAction(String action, Bundle extraBundle) {
        if (TextUtils.isEmpty(action)) {
            Log.d(LOG_TAG, "No action available with Intent. Open the app.");

            // No specific action found.
            // Open the app with extra bundle.
            openApp(extraBundle);
        } else {
            Log.d(LOG_TAG, "Action available: " + action);

            // Process the action
            startService(action, extraBundle);
        }
    }

    private void openApp(Bundle extraBundle) {
        PackageManager packageManager = getPackageManager();
        Intent launcherIntent = packageManager.getLaunchIntentForPackage(getPackageName());

        // add the whole bundle. typically contains message object.
        if (launcherIntent != null) {
            if (extraBundle != null) {
                launcherIntent.putExtras(extraBundle);
            }

            launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launcherIntent);

            if (extraBundle != null) {
                Message message = (Message) extraBundle.getSerializable(RichPushConstants.EXTRA_MESSAGE);
                Blueshift
                        .getInstance(getApplicationContext())
                        .trackNotificationPageOpen(message, true);
            }
        } else {
            Log.w(LOG_TAG, "No MAIN Activity found in AndroidManifext.xml");
        }
    }

    private void startService(String action, Bundle extraBundle) {
        Intent serviceIntent = findServiceAndCreateIntent(action, extraBundle);
        if (serviceIntent != null) {
            startService(serviceIntent);
        } else {
            Log.d(LOG_TAG, "No service declared in AndroidManifest.xml for action com.blueshift.NOTIFICATION_CLICK_EVENT");
        }
    }

    private Intent findServiceAndCreateIntent(String action, Bundle extraBundle) {
        Intent serviceToStart = null;

        // search for service that handles notification clicks (custom or built-in)
        Intent serviceIntent = new Intent();
        serviceIntent.setAction("com.blueshift.NOTIFICATION_CLICK_EVENT");
        serviceIntent.setPackage(getPackageName());

        List<ResolveInfo> resInfo = getPackageManager().queryIntentServices(serviceIntent, 0);
        if (resInfo != null && !resInfo.isEmpty()) {
            // read default service
            ServiceInfo service;

            // check if service is overridden
            if (resInfo.size() == 1) {
                service = resInfo.get(0).serviceInfo;
            } else {
                Log.d(LOG_TAG, "Declared more than one service to receive this action.");

                service = getCustomServiceInfo(resInfo);
            }

            ComponentName cmpService = new ComponentName(service.applicationInfo.packageName, service.name);

            serviceToStart = new Intent();
            serviceToStart.setAction(action);
            serviceToStart.putExtras(extraBundle);
            serviceToStart.setComponent(cmpService);
        }

        return serviceToStart;
    }

    private ServiceInfo getCustomServiceInfo(List<ResolveInfo> resolveInfos) {
        ServiceInfo info = null;

        if (resolveInfos != null) {
            String serviceName = BlueshiftNotificationEventsService.class.getName();

            for (ResolveInfo resInfo : resolveInfos) {
                Log.d(LOG_TAG, "Service Name: " + resInfo.serviceInfo.name);

                if (!resInfo.serviceInfo.name.equals(serviceName)) {
                    info = resInfo.serviceInfo;
                    break;
                }
            }
        }

        return info;
    }
}
