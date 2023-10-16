package com.blueshift.util;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import org.junit.Test;

public class BlueshiftUtilsTest {
    @Test
    public void testRemoveQueryParam_RemovesExistingQueryParamBeginning() {
        String url = "https://example.com/page?param1=value1&param2=value2&param3=value3";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertThat("https://example.com/page?param2=value2&param3=value3").isEqualTo(modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_RemovesExistingQueryParamMiddle() {
        String url = "https://example.com/page?param1=value1&param2=value2&param3=value3";
        String paramToRemove = "param2";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertThat("https://example.com/page?param1=value1&param3=value3").isEqualTo(modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_RemovesExistingQueryParamEnd() {
        String url = "https://example.com/page?param1=value1&param2=value2&param3=value3";
        String paramToRemove = "param3";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertThat("https://example.com/page?param1=value1&param2=value2").isEqualTo(modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveNonExistingQueryParam() {
        String url = "https://example.com/page?param1=value1&param3=value3";
        String paramToRemove = "param2";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertThat(url).isEqualTo(modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveQueryParamFromEmptyURL() {
        String url = "";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertThat(url).isEqualTo(modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveQueryParamFromOpaqueURI() {
        String url = "mailto:test@example.com";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertThat(url).isEqualTo(modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveTheFirstQueryParam() {
        String url = "https://example.com/page?param1=value1&param3=value3";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertThat("https://example.com/page?param3=value3").isEqualTo(modifiedUri.toString());
    }

    @Test
    public void testRemoveQueryParam_TryToRemoveTheOnlyQueryParam() {
        String url = "https://example.com/page?param1=value1";
        String paramToRemove = "param1";
        Uri modifiedUri = BlueshiftUtils.removeQueryParam(paramToRemove, Uri.parse(url));
        assertThat("https://example.com/page").isEqualTo(modifiedUri.toString());
    }

    @Test
    public void testShouldOpenURLWithExternalApp_WithNullUri() {
        // Test when the Uri is null
        Uri data = null;
        assertThat(BlueshiftUtils.shouldOpenURLWithExternalApp(data)).isFalse();
    }

    @Test
    public void testShouldOpenURLWithExternalApp_WithOpenInBrowser() {
        // Test when the "bsft_tgt" query parameter is "browser"
        Uri data = Uri.parse("https://example.com/?bsft_tgt=browser");
        assertThat(BlueshiftUtils.shouldOpenURLWithExternalApp(data)).isTrue();
    }

    @Test
    public void testShouldOpenURLWithExternalApp_WithOpenInOtherApp() {
        // Test when the "bsft_tgt" query parameter is not "browser"
        Uri data = Uri.parse("https://example.com/?bsft_tgt=email");
        assertThat(BlueshiftUtils.shouldOpenURLWithExternalApp(data)).isFalse();
    }

    @Test
    public void testShouldOpenURLWithExternalApp_WithoutQueryParameter() {
        // Test when the Uri has no "bsft_tgt" query parameter
        Uri data = Uri.parse("https://example.com/");
        assertThat(BlueshiftUtils.shouldOpenURLWithExternalApp(data)).isFalse();
    }
}