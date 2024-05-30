package com.blueshift;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.blueshift.util.CommonUtils;

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;

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
        List<Object> result = blueshift.inferAppVersionChangeEvent(context);

        // assert that the item in 0th index of result is "app_install"
        assertEquals("app_install", result.get(0));

        // assert that the hashmap in 1st index of result has key "app_installed_at"
        HashMap<String, Object> map = (HashMap<String, Object>) result.get(1);
        assertEquals("app_installed_at", map.keySet().iterator().next());
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
        List<Object> result = blueshift.inferAppVersionChangeEvent(context);

        // assert that the item in 0th index of result is "app_update"
        assertEquals("app_update", result.get(0));

        // assert that the hashmap in 1st index of result has key "app_updated_at"
        HashMap<String, Object> map = (HashMap<String, Object>) result.get(1);
        assertEquals("app_updated_at", map.keySet().iterator().next());
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
        List<Object> result = blueshift.inferAppVersionChangeEvent(context);

        // assert that the item in 0th index of result is "app_install"
        assertEquals("app_install", result.get(0));

        // assert that the hashmap in 1st index of result has key "app_installed_at"
        HashMap<String, Object> map = (HashMap<String, Object>) result.get(1);
        assertEquals("app_installed_at", map.keySet().iterator().next());
    }
}
