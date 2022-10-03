package com.blueshift.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;

import com.blueshift.BlueshiftAdIdProvider;
import com.blueshift.BlueshiftAttributesApp;
import com.blueshift.BlueshiftLogger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author Rahul Raveendran V P
 * Created on 5/3/15 @ 2:00 PM
 * https://github.com/rahulrvp
 */
public class DeviceUtils {
    private static final String LOG_TAG = DeviceUtils.class.getSimpleName();

    public static String getSIMOperatorName(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return telephonyManager.getSimOperatorName();
        }

        return null;
    }

    private static void installNewGooglePlayServicesApp(Context context) {
        try {
            Intent gpsInstallIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms"));
            gpsInstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(gpsInstallIntent);
        } catch (ActivityNotFoundException e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    /**
     * Returns the cached device_id based on the device_id source specified during initialization.
     *
     * @param context Nullable context variable.
     * @return Valid device_id if available at the time of call. Else, returns null.
     */
    public static String getDeviceId(Context context) {
        return BlueshiftAttributesApp.getInstance().getCachedDeviceId();
    }

    public static String getAdvertisingId(Context context) {
        return BlueshiftAdIdProvider.getInstance(context).getId();
    }

    public static boolean isLimitAdTrackingEnabled(Context context) {
        return BlueshiftAdIdProvider.getInstance(context).isLimitAdTrackingEnabled();
    }

    public static String getIP4Address() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    Object inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
                        return ((Inet4Address) inetAddress).getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return null;
    }
}
