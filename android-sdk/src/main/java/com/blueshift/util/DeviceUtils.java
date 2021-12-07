package com.blueshift.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;

import com.blueshift.BlueshiftAttributesApp;
import com.blueshift.BlueshiftLogger;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

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
        String advertisingId = null;

        try {
            AdvertisingIdClient.Info info = getAdvertisingIdClientInfo(context);
            if (info != null) {
                advertisingId = info.getId();
            }

            if (isLimitAdTrackingEnabled(context)) {
                BlueshiftLogger.w(LOG_TAG, "Limit-Ad-Tracking is enabled by the user.");
            }
        } catch (Exception e) {
            BlueshiftLogger.e(LOG_TAG, e);
        }

        return advertisingId;
    }

    public static boolean isLimitAdTrackingEnabled(Context context) {
        boolean status = true; // by default the opt-out is turned ON

        AdvertisingIdClient.Info info = getAdvertisingIdClientInfo(context);
        if (info != null) {
            status = info.isLimitAdTrackingEnabled();
        }

        return status;
    }

    private static AdvertisingIdClient.Info getAdvertisingIdClientInfo(Context context) {
        AdvertisingIdClient.Info info = null;

        if (context != null) {
            try {
                info = AdvertisingIdClient.getAdvertisingIdInfo(context);
            } catch (Exception e) {
                BlueshiftLogger.e(LOG_TAG, e);
            }
        }

        return info;
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
