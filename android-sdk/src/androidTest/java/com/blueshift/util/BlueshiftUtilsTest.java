package com.blueshift.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import org.junit.Test;

public class BlueshiftUtilsTest {
    @Test
    public void testRemoveQueryParam_RemovesExistingQueryParam() {
        String url = "https://example.com/page?param1=value1&param2=value2&param3=value3";
        String paramToRemove = "param2";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertEquals("https://example.com/page?param1=value1&param3=value3", modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveNonExistingQueryParam() {
        String url = "https://example.com/page?param1=value1&param3=value3";
        String paramToRemove = "param2";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertEquals(url, modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveQueryParamFromEmptyURL() {
        String url = "";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertEquals("", modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveQueryParamFromOpaqueURI() {
        String url = "mailto:test@example.com";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertEquals(url, modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveTheFirstQueryParam() {
        String url = "https://example.com/page?param1=value1&param3=value3";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertEquals("https://example.com/page?param3=value3", modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveTheOnlyQueryParam() {
        String url = "https://example.com/page?param1=value1";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertEquals("https://example.com/page", modifiedUri.toString());
    }

    @Test
    public void testShouldOpenURLWithExternalApp_WithNullUri() {
        // Test when the Uri is null
        Uri data = null;
        assertFalse(BlueshiftUtils.shouldOpenURLWithExternalApp(data));
    }

    @Test
    public void testShouldOpenURLWithExternalApp_WithOpenInBrowser() {
        // Test when the "open-in" query parameter is "browser"
        Uri data = Uri.parse("https://example.com/?open-in=browser");
        assertTrue(BlueshiftUtils.shouldOpenURLWithExternalApp(data));
    }

    @Test
    public void testShouldOpenURLWithExternalApp_WithOpenInOtherApp() {
        // Test when the "open-in" query parameter is not "browser"
        Uri data = Uri.parse("https://example.com/?open-in=email");
        assertFalse(BlueshiftUtils.shouldOpenURLWithExternalApp(data));
    }

    @Test
    public void testShouldOpenURLWithExternalApp_WithoutQueryParameter() {
        // Test when the Uri has no "open-in" query parameter
        Uri data = Uri.parse("https://example.com/");
        assertFalse(BlueshiftUtils.shouldOpenURLWithExternalApp(data));
    }
}