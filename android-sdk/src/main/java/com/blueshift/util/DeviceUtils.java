package com.blueshift.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.WorkerThread;
import android.telephony.TelephonyManager;

import com.blueshift.BlueShiftPreference;
import com.blueshift.BlueshiftLogger;
import com.blueshift.R;
import com.blueshift.model.Configuration;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.firebase.iid.FirebaseInstanceId;

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
            BlueshiftLogger.e(LOG_TAG, e);
        }
    }

    /**
     * This method is responsible for returning a valid device_id string.
     *
     * Depending upon if the developer has enabled collection of Advertising Id
     * as device_id, it will return advertising id or GUID.
     *
     * As the Advertising Id should be collected from worker thread, this method
     * should be called from worker thread only.
     *
     * @param context Valid {@link Context} object
     * @return Valid device_id string
     */
    @WorkerThread
    public static String getDeviceId(Context context) {
        String deviceId = null;

        Configuration configuration = BlueshiftUtils.getConfiguration(context);
        if (configuration != null && configuration.getDeviceIdSource() != null) {
            switch (configuration.getDeviceIdSource()) {
                case INSTANCE_ID:
                    deviceId = FirebaseInstanceId.getInstance().getId();
                    break;
                case GUID:
                    deviceId = BlueShiftPreference.getDeviceID(context);
                    break;
                case ADVERTISING_ID_PKG_NAME:
                    try {
                        deviceId = getAdvertisingId(context);
                        deviceId += (":" + context.getPackageName());
                    } catch (Exception e) {
                        BlueshiftLogger.e(LOG_TAG, "Could not build \"advertising id - pkg name\" combo.");
                        BlueshiftLogger.e(LOG_TAG, e);
                    }
                    break;
                case INSTANCE_ID_PKG_NAME:
                    try {
                        deviceId = FirebaseInstanceId.getInstance().getId();
                        deviceId += (":" + context.getPackageName());
                    } catch (Exception e) {
                        BlueshiftLogger.e(LOG_TAG, "Could not build \"instance id - pkg name\" combo.");
                        BlueshiftLogger.e(LOG_TAG, e);
                    }
                    break;
                default:
                    // ADVERTISING_ID & Others
                    deviceId = getAdvertisingId(context);
            }
        } else {
            deviceId = getAdvertisingId(context);
        }

        return deviceId;
    }

    private static String getAdvertisingId(Context context) {
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
