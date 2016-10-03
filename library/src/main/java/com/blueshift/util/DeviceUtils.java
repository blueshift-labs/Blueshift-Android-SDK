package com.blueshift.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

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
        Toast.makeText(
                context,
                R.string.install_gps_app_toast_msg,
                Toast.LENGTH_SHORT).show();

        Intent gpsInstallIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms"));
        gpsInstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(gpsInstallIntent);
    }

    public static String getAdvertisingID(Context context) {
        String advertisingId = null;

        String libNotFoundMessage = context.getString(R.string.gps_not_found_msg);

        try {
            AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(context);
            if (info != null) {
                if (info.isLimitAdTrackingEnabled()) {
                    Log.w(LOG_TAG, "User has limit ad tracking enabled.");
                } else {
                    advertisingId = info.getId();
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, libNotFoundMessage + "\n" + e.getMessage());
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(LOG_TAG, libNotFoundMessage + "\n" + e.getMessage());

            installNewGooglePlayServicesApp(context);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return advertisingId;
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
            Log.e(LOG_TAG, e.getMessage());
        }

        return null;
    }
}
