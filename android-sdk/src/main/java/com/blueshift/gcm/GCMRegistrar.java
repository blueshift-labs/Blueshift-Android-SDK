/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blueshift.gcm;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilities for device registration.
 * <p>
 * <strong>Note:</strong> this class uses a private {@link SharedPreferences}
 * object to keep track of the registration token.
 */
public final class GCMRegistrar {

    /**
     * Default lifespan (7 days) of the {@link #isRegisteredOnServer(Context)}
     * flag until it is considered expired.
     */
    // NOTE: cannot use TimeUnit.DAYS because it's not available on API Level 8
    public static final long DEFAULT_ON_SERVER_LIFESPAN_MS =
            1000 * 3600 * 24 * 7;

    private static final String TAG = "GCMRegistrar";
    private static final String BACKOFF_MS = "backoff_ms";
    private static final String GSF_PACKAGE = "com.google.android.gsf";
    private static final String PREFERENCES = "com.blueshift.gcm";
    private static final int DEFAULT_BACKOFF_MS = 3000;
    private static final String PROPERTY_REG_ID = "regId";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER = "onServer";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME =
            "onServerExpirationTime";
    private static final String PROPERTY_ON_SERVER_LIFESPAN =
            "onServerLifeSpan";

    private static String mSenderId;

    /**
     * {@link GCMBroadcastReceiver} instance used to handle the retry intent.
     * <p>
     * <p>
     * This instance cannot be the same as the one defined in the manifest
     * because it needs a different permission.
     */
    private static GCMBroadcastReceiver sRetryReceiver;

    private static String sRetryReceiverClassName;

    private GCMRegistrar() {
        throw new UnsupportedOperationException();
    }

    public static void registerForNotification(Context context) {
        try {
            if (checkDevice(context) && checkManifest(context)) {
                // Always ask for a brand new Token.
                Log.d("GCMIntentService", "Trying to register for GCM");
                register(context, mSenderId);

                /*
                String regId = getRegistrationId(context);
                if (regId.isEmpty()) {
                    Log.d("GCMIntentService", "Trying to register for GCM");
                    register(context, mSenderId);
                } else {
                    Log.v(TAG, "Already registered. id: " + regId);
                }
                */
            } else {
                Log.e(TAG, "GCM registration failed.");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Checks if the device has the proper dependencies installed.
     * <p>
     * This method should be called when the application starts to verify that
     * the device supports GCM.
     *
     * @param context application context.
     * @return {@link Boolean}
     * @throws UnsupportedOperationException if the device does not support GCM.
     */
    public static boolean checkDevice(Context context) {
        try {
            int version = Build.VERSION.SDK_INT;
            if (version < 8) {
                Log.e(TAG, "Device must be at least API Level 8 (instead of " + version + ")");
                return false;
            }
            PackageManager packageManager = context.getPackageManager();
            try {
                if (packageManager != null) {
                    packageManager.getPackageInfo(GSF_PACKAGE, 0);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Device does not have package " + GSF_PACKAGE);
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());

            return false;
        }
    }

    /**
     * Checks that the application manifest is properly configured.
     * <p>
     * A proper configuration means:
     * <ol>
     * <li>It creates a custom permission called
     * {@code PACKAGE_NAME.permission.C2D_MESSAGE}.
     * <li>It defines at least one {@link android.content.BroadcastReceiver} with category
     * {@code PACKAGE_NAME}.
     * <li>The {@link android.content.BroadcastReceiver}(s) uses the
     * {@value GCMConstants#PERMISSION_GCM_INTENTS} permission.
     * <li>The {@link android.content.BroadcastReceiver}(s) handles the 3 GCM intents
     * ({@value GCMConstants#INTENT_FROM_GCM_MESSAGE},
     * {@value GCMConstants#INTENT_FROM_GCM_REGISTRATION_CALLBACK},
     * and {@value GCMConstants#INTENT_FROM_GCM_LIBRARY_RETRY}).
     * </ol>
     * ...where {@code PACKAGE_NAME} is the application package.
     * <p>
     * This method should be used during development time to verify that the
     * manifest is properly set up, but it doesn't need to be called once the
     * application is deployed to the users' devices.
     *
     * @param context application context.
     * @return {@link Boolean}
     * @throws IllegalStateException if any of the conditions above is not met.
     */
    public static boolean checkManifest(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            String permissionName = packageName + ".permission.C2D_MESSAGE";
            // check permission
            try {
                if (packageManager != null) {
                    packageManager.getPermissionInfo(permissionName, PackageManager.GET_PERMISSIONS);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Application does not define permission " + permissionName);
                return false;
            }

            // check receivers
            PackageInfo receiversInfo = null;
            try {
                if (packageManager != null) {
                    receiversInfo = packageManager.getPackageInfo(
                            packageName, PackageManager.GET_RECEIVERS);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Could not get receivers for package " + packageName);
                return false;
            }

            if (receiversInfo == null
                    || receiversInfo.receivers == null || receiversInfo.receivers.length == 0) {
                Log.e(TAG, "No receiver for package " + packageName);
                return false;
            }

            ActivityInfo[] receivers = receiversInfo.receivers;
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "number of receivers for " + packageName + ": " +
                        receivers.length);
            }
            Set<String> allowedReceivers = new HashSet<>();
            for (ActivityInfo receiver : receivers) {
                if (GCMConstants.PERMISSION_GCM_INTENTS.equals(
                        receiver.permission)) {
                    allowedReceivers.add(receiver.name);
                }
            }
            if (allowedReceivers.isEmpty()) {
                Log.e(TAG, "No receiver allowed to receive " + GCMConstants.PERMISSION_GCM_INTENTS);
                return false;
            }
            checkReceiver(context, allowedReceivers,
                    GCMConstants.INTENT_FROM_GCM_REGISTRATION_CALLBACK);
            checkReceiver(context, allowedReceivers,
                    GCMConstants.INTENT_FROM_GCM_MESSAGE);

            // check for sender Id
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                if (applicationInfo != null) {
                    Bundle bundle = applicationInfo.metaData;
                    if (bundle == null || bundle.keySet() == null || !bundle.keySet().contains(GCMConstants.KEY_SENDER_ID)) {
                        Log.e(TAG, "Please add sender id in AndroidManifest.xml as meta-data with name " + GCMConstants.KEY_SENDER_ID);
                        return false;
                    } else {
                        mSenderId = bundle.getString(GCMConstants.KEY_SENDER_ID);
                        if (mSenderId == null || mSenderId.isEmpty()) {
                            Log.e(TAG, "Please provide a valid GCM sender ID");
                            return false;
                        } else {
                            // this line is to remove the id: from meta-data value.
                            mSenderId = mSenderId.substring(3);
                        }
                    }
                }
            } catch (NameNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());

            return false;
        }
    }

    private static void checkReceiver(Context context, Set<String> allowedReceivers, String action) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            Intent intent = new Intent(action);
            intent.setPackage(packageName);
            List<ResolveInfo> receivers = pm.queryBroadcastReceivers(intent,
                    PackageManager.GET_INTENT_FILTERS);
            if (receivers.isEmpty()) {
                throw new IllegalStateException("No receivers for action " +
                        action);
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Found " + receivers.size() + " receivers for action " +
                        action);
            }
            // make sure receivers match
            for (ResolveInfo receiver : receivers) {
                String name = receiver.activityInfo.name;
                if (!allowedReceivers.contains(name)) {
                    throw new IllegalStateException("Receiver " + name +
                            " is not set with permission " +
                            GCMConstants.PERMISSION_GCM_INTENTS);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Initiate messaging registration for the current application.
     * <p>
     * The result will be returned as an
     * {@link GCMConstants#INTENT_FROM_GCM_REGISTRATION_CALLBACK} intent with
     * either a {@link GCMConstants#EXTRA_REGISTRATION_ID} or
     * {@link GCMConstants#EXTRA_ERROR}.
     *
     * @param context   application context.
     * @param senderIds Google Project ID of the accounts authorized to send
     *                  messages to this application.
     * @throws IllegalStateException if device does not have all GCM
     *                               dependencies installed.
     */
    public static void register(Context context, String... senderIds) {
        try {
            GCMRegistrar.resetBackoff(context);
            internalRegister(context, senderIds);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    static void internalRegister(Context context, String... senderIds) {
        try {
            String flatSenderIds = getFlatSenderIds(senderIds);
            Log.v(TAG, "Registering app " + context.getPackageName() +
                    " of senders " + flatSenderIds);
            Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_REGISTRATION);
            intent.setPackage(GSF_PACKAGE);
            intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
                    PendingIntent.getBroadcast(context, 0, new Intent(), 0));
            intent.putExtra(GCMConstants.EXTRA_SENDER, flatSenderIds);
            context.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    static String getFlatSenderIds(String... senderIds) {
        try {
            if (senderIds == null || senderIds.length == 0) {
                throw new IllegalArgumentException("No senderIds");
            }

            StringBuilder builder = new StringBuilder();
            boolean isFirst = true;

            for (String senderId : senderIds) {
                if (senderId != null) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        builder.append(",");
                    }

                    builder.append(senderId);
                }
            }

            /*
            // Old code for reference
            StringBuilder builder = new StringBuilder(senderIds[0]);
            for (int i = 1; i < senderIds.length; i++) {
                builder.append(',').append(senderIds[i]);
            }
            return builder.toString();
            */

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return "";
    }

    /**
     * Unregister the application.
     * <p>
     * The result will be returned as an
     * {@link GCMConstants#INTENT_FROM_GCM_REGISTRATION_CALLBACK} intent with an
     * {@link GCMConstants#EXTRA_UNREGISTERED} extra.
     *
     * @param context {@link Context}
     */
    public static void unregister(Context context) {
        try {
            GCMRegistrar.resetBackoff(context);
            internalUnregister(context);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Clear internal resources.
     * <p>
     * <p>
     * This method should be called by the main activity's {@code onDestroy()}
     * method.
     *
     * @param context {@link Context}
     */
    public static synchronized void onDestroy(Context context) {
        try {
            if (sRetryReceiver != null) {
                Log.v(TAG, "Unregistering receiver");
                context.unregisterReceiver(sRetryReceiver);
                sRetryReceiver = null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    static void internalUnregister(Context context) {
        try {
            Log.v(TAG, "Unregistering app " + context.getPackageName());
            Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_UNREGISTRATION);
            intent.setPackage(GSF_PACKAGE);
            intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
                    PendingIntent.getBroadcast(context, 0, new Intent(), 0));
            context.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Lazy initializes the {@link GCMBroadcastReceiver} instance.
     */
    static synchronized void setRetryBroadcastReceiver(Context context) {
        try {
            if (sRetryReceiver == null) {
                if (sRetryReceiverClassName == null) {
                    // should never happen
                    Log.e(TAG, "internal error: retry receiver class not set yet");
                    sRetryReceiver = new GCMBroadcastReceiver();
                } else {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(sRetryReceiverClassName);
                        sRetryReceiver = (GCMBroadcastReceiver) clazz.newInstance();
                    } catch (Exception e) {
                        Log.e(TAG, "Could not create instance of " +
                                sRetryReceiverClassName + ". Using " +
                                GCMBroadcastReceiver.class.getName() +
                                " directly.");
                        sRetryReceiver = new GCMBroadcastReceiver();
                    }
                }
                String category = context.getPackageName();
                IntentFilter filter = new IntentFilter(
                        GCMConstants.INTENT_FROM_GCM_LIBRARY_RETRY);
                filter.addCategory(category);
                // must use a permission that is defined on manifest for sure
                String permission = category + ".permission.C2D_MESSAGE";
                Log.v(TAG, "Registering receiver");
                context.registerReceiver(sRetryReceiver, filter, permission, null);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Sets the name of the retry receiver class.
     */
    static void setRetryReceiverClassName(String className) {
        Log.v(TAG, "Setting the name of retry receiver class to " + className);
        sRetryReceiverClassName = className;
    }

    /**
     * Gets the current registration id for application on GCM service.
     * <p>
     * If result is empty, the registration has failed.
     *
     * @param context {@link Context}
     * @return registration id, or empty string if the registration is not
     * complete.
     */
    public static String getRegistrationId(Context context) {
        try {
            final SharedPreferences prefs = getGCMPreferences(context);
            if (prefs != null) {
                String registrationId = prefs.getString(PROPERTY_REG_ID, "");
                // check if app was updated; if so, it must clear registration id to
                // avoid a race condition if GCM sends a message
                int oldVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
                int newVersion = getAppVersion(context);
                if (oldVersion != Integer.MIN_VALUE && oldVersion != newVersion) {
                    Log.v(TAG, "App version changed from " + oldVersion + " to " +
                            newVersion + "; resetting registration id");
                    clearRegistrationId(context);
                    registrationId = "";
                }

                return registrationId;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return "";
    }

    /**
     * Checks whether the application was successfully registered on GCM
     * service.
     */
    public static boolean isRegistered(Context context) {
        return getRegistrationId(context).length() > 0;
    }

    /**
     * Clears the registration id in the persistence store.
     *
     * @param context application's context.
     * @return old registration id.
     */
    static String clearRegistrationId(Context context) {
        return setRegistrationId(context, "");
    }

    /**
     * Sets the registration id in the persistence store.
     *
     * @param context application's context.
     * @param regId   registration id
     */
    static String setRegistrationId(Context context, String regId) {
        try {
            final SharedPreferences prefs = getGCMPreferences(context);
            if (prefs != null) {
                String oldRegistrationId = prefs.getString(PROPERTY_REG_ID, "");
                int appVersion = getAppVersion(context);
                Log.v(TAG, "Saving regId on app version " + appVersion);
                Editor editor = prefs.edit();
                editor.putString(PROPERTY_REG_ID, regId);
                editor.putInt(PROPERTY_APP_VERSION, appVersion);
                editor.apply();
                return oldRegistrationId;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return "";
    }

    /**
     * Sets whether the device was successfully registered in the server side.
     */
    public static void setRegisteredOnServer(Context context, boolean flag) {
        try {
            final SharedPreferences prefs = getGCMPreferences(context);
            if (prefs != null) {
                Editor editor = prefs.edit();
                editor.putBoolean(PROPERTY_ON_SERVER, flag);
                // set the flag's expiration date
                long lifespan = getRegisterOnServerLifespan(context);
                long expirationTime = System.currentTimeMillis() + lifespan;
                Log.v(TAG, "Setting registeredOnServer status as " + flag + " until " +
                        new Timestamp(expirationTime));
                editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Checks whether the device was successfully registered in the server side,
     * as set by {@link #setRegisteredOnServer(Context, boolean)}.
     * <p>
     * <p>To avoid the scenario where the device sends the registration to the
     * server but the server loses it, this flag has an expiration date, which
     * is {@link #DEFAULT_ON_SERVER_LIFESPAN_MS} by default (but can be changed
     * by {@link #setRegisterOnServerLifespan(Context, long)}).
     */
    public static boolean isRegisteredOnServer(Context context) {
        try {
            boolean isRegistered = false;
            final SharedPreferences prefs = getGCMPreferences(context);
            if (prefs != null) {
                isRegistered = prefs.getBoolean(PROPERTY_ON_SERVER, false);
                Log.v(TAG, "Is registered on server: " + isRegistered);
                if (isRegistered) {
                    // checks if the information is not stale
                    long expirationTime =
                            prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
                    if (System.currentTimeMillis() > expirationTime) {
                        Log.v(TAG, "flag expired on: " + new Timestamp(expirationTime));
                        return false;
                    }
                }
            }

            return isRegistered;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return false;
    }

    /**
     * Gets how long (in milliseconds) the {@link #isRegistered(Context)}
     * property is valid.
     *
     * @return value set by {@link #setRegisteredOnServer(Context, boolean)} or
     * {@link #DEFAULT_ON_SERVER_LIFESPAN_MS} if not set.
     */
    public static long getRegisterOnServerLifespan(Context context) {
        try {
            final SharedPreferences prefs = getGCMPreferences(context);
            long lifespan = DEFAULT_ON_SERVER_LIFESPAN_MS;
            if (prefs != null) {
                lifespan = prefs.getLong(PROPERTY_ON_SERVER_LIFESPAN,
                        DEFAULT_ON_SERVER_LIFESPAN_MS);
            }

            return lifespan;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return DEFAULT_ON_SERVER_LIFESPAN_MS;
    }

    /**
     * Sets how long (in milliseconds) the {@link #isRegistered(Context)}
     * flag is valid.
     */
    public static void setRegisterOnServerLifespan(Context context, long lifespan) {
        try {
            final SharedPreferences prefs = getGCMPreferences(context);
            if (prefs != null) {
                Editor editor = prefs.edit();
                editor.putLong(PROPERTY_ON_SERVER_LIFESPAN, lifespan);
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Gets the application version.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Coult not get package name: " + e);
        }
    }

    /**
     * Resets the backoff counter.
     * <p>
     * This method should be called after a GCM call succeeds.
     *
     * @param context application's context.
     */
    static void resetBackoff(Context context) {
        try {
            Log.d(TAG, "resetting backoff for " + context.getPackageName());
            setBackoff(context, DEFAULT_BACKOFF_MS);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Gets the current backoff counter.
     *
     * @param context application's context.
     * @return current backoff counter, in milliseconds.
     */
    static int getBackoff(Context context) {
        try {
            final SharedPreferences prefs = getGCMPreferences(context);
            if (prefs != null) {
                return prefs.getInt(BACKOFF_MS, DEFAULT_BACKOFF_MS);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return DEFAULT_BACKOFF_MS;
    }

    /**
     * Sets the backoff counter.
     * <p>
     * This method should be called after a GCM call fails, passing an
     * exponential value.
     *
     * @param context application's context.
     * @param backoff new backoff counter, in milliseconds.
     */
    static void setBackoff(Context context, int backoff) {
        try {
            final SharedPreferences prefs = getGCMPreferences(context);
            if (prefs != null) {
                Editor editor = prefs.edit();
                editor.putInt(BACKOFF_MS, backoff);
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static SharedPreferences getGCMPreferences(Context context) {
        try {
            return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }

    public static String getGCMSenderId() {
        return mSenderId;
    }
}
