package com.blueshift;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.blueshift.util.CommonUtils;

import org.junit.Test;

import java.io.File;
import java.util.HashMap;

public class BlueshiftTest {
    @Test
    public void testFreshAppInstall() {
        Context context = mock(Context.class);

        // stored version is unavailable
        when(BlueShiftPreference.getStoredAppVersionString(context)).thenReturn(null);

        // database file is unavailable
        File database = mock(File.class);
        when(context.getDatabasePath("blueshift_db.sqlite3")).thenReturn(database);
        when(database.exists()).thenReturn(false);

        // app version is available
        when(CommonUtils.getAppVersion(context)).thenReturn("1.0.0");

        Blueshift blueshift = Blueshift.getInstance(context);
        blueshift.doAppVersionChecks(context);

        // confirm if the sendEvent was called with event name app_install
        assertFalse(blueshift.sendEvent("app_install", null, false));
    }

    @Test
    public void testAppVersionUpgradeOldSDKToNewSDK() {
        Context context = mock(Context.class);

        // stored version is unavailable
        when(BlueShiftPreference.getStoredAppVersionString(context)).thenReturn(null);

        // database file is available
        File database = mock(File.class);
        when(context.getDatabasePath("blueshift_db.sqlite3")).thenReturn(database);
        when(database.exists()).thenReturn(true);

        // app version is available
        when(CommonUtils.getAppVersion(context)).thenReturn("2.0.0");

        Blueshift blueshift = Blueshift.getInstance(context);
        blueshift.doAppVersionChecks(context);

        // confirm if the sendEvent was called with event name app_update
        assertFalse(blueshift.sendEvent("app_update", null, false));
    }

    @Test
    public void testAppVersionUpgradeNoSDKToNewSDK() {
        Context context = mock(Context.class);

        // stored version is available
        when(BlueShiftPreference.getStoredAppVersionString(context)).thenReturn(null);

        // database file is available
        File database = mock(File.class);
        when(context.getDatabasePath("blueshift_db.sqlite3")).thenReturn(database);
        when(database.exists()).thenReturn(false);

        // app version is available
        when(CommonUtils.getAppVersion(context)).thenReturn("2.0.0");

        Blueshift blueshift = Blueshift.getInstance(context);
        blueshift.doAppVersionChecks(context);

        // confirm if the sendEvent was called with event name app_update
        assertFalse(blueshift.sendEvent("app_update", null, false));
    }
}