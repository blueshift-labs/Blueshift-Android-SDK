package com.blueshift.Util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by rahul on 5/3/15.
 */
public class DeviceUtils {
    private static final String LOG_TAG = DeviceUtils.class.getSimpleName();

    public static String getAndroidID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getSIMOperatorName(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return telephonyManager.getSimOperatorName();
        }

        return null;
    }

    public static String getAdvertisingID(Context context) {
        String advertisingId = null;
        String libNotFoundMessage = "Could not fetch AdvertisingId. Please make sure that Google Play services 4.0+ is included in your project. Visit \"https://developer.android.com/google/play-services/setup.html\" for help.";

        try {
            Class advertisingIdClientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            Method getAdvertisingInfoMethod = advertisingIdClientClass.getMethod("getAdvertisingIdInfo", Context.class);
            Object infoObject = getAdvertisingInfoMethod.invoke(null, context);
            Class infoClass = Class.forName(infoObject.getClass().getName());

            Method isLimitAdTrackingEnabledMethod = infoClass.getDeclaredMethod("isLimitAdTrackingEnabled");
            isLimitAdTrackingEnabledMethod.setAccessible(true);
            Object status = isLimitAdTrackingEnabledMethod.invoke(infoObject);
            if (Boolean.valueOf(status.toString())) {
                Log.w(LOG_TAG, "Ad tracking is limited by user.");
            }

            Method getIdMethod = infoClass.getDeclaredMethod("getId");
            getIdMethod.setAccessible(true);
            Object id = getIdMethod.invoke(infoObject);
            advertisingId = String.valueOf(id);
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, libNotFoundMessage + "\nClass not found: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(LOG_TAG, libNotFoundMessage);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e(LOG_TAG, libNotFoundMessage);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, libNotFoundMessage);
            e.printStackTrace();
        } catch (Exception e) {
            /**
             * If control falls here, then it means that the user has the Google Play services lib added in the project, but
             * the device does not have a supporting version of Google Play services app installed. The following
             * code will prompt the device user to install new version of Google Play services.
             */
            if (context != null) {
                Toast.makeText(context, "Please install/update Google Play services.", Toast.LENGTH_SHORT).show();
                Intent gpsInstallIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms"));
                gpsInstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(gpsInstallIntent);
            }
            e.printStackTrace();
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
        } catch (SocketException e) { e.printStackTrace(); }

        return null;
    }
}
