package com.blueshift.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.blueshift.R;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author Rahul Raveendran V P
 *         Created on 5/3/15 @ 2:00 PM
 *         https://github.com/rahulrvp
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
            Log.e(LOG_TAG, e.getMessage() != null ? e.getMessage() : "Unknown error!");
        }
    }

    public static String getAdvertisingID(Context context) {
        String advertisingId = null;

        if (!isLimitAdTrackingEnabled(context)) {
            AdvertisingIdClient.Info info = getAdvertisingIdClientInfo(context);
            if (info != null) {
                advertisingId = info.getId();
            }
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
            String libNotFoundMessage = context.getString(R.string.gps_not_found_msg);

            try {
                info = AdvertisingIdClient.getAdvertisingIdInfo(context);
            } catch (IOException e) {
                String logMessage = e.getMessage() != null ? e.getMessage() : "";
                SdkLog.e(LOG_TAG, libNotFoundMessage + "\n" + logMessage);
            } catch (GooglePlayServicesNotAvailableException | IllegalStateException e) {
                Log.e(LOG_TAG, libNotFoundMessage);
                installNewGooglePlayServicesApp(context);
            } catch (GooglePlayServicesRepairableException e) {
                SdkLog.e(LOG_TAG, e.getMessage());
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
            SdkLog.e(LOG_TAG, e.getMessage());
        }

        return null;
    }
}
