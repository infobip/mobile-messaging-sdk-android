/*
 * MobileApiResourceProviderTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.UserAgentAdditions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * @author sslavin
 * @since 27/11/2017.
 */

public class MobileApiResourceProviderTest extends MobileMessagingTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mobileApiResourceProvider = new MobileApiResourceProvider();
    }

//    @Test
//    public void shouldSaveBaseUrlFromResponse() {
//        // given
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null, new HashMap<String, String>() {{
//            put(CustomApiHeaders.NEW_BASE_URL.getValue(), "customUrl");
//        }});
//
//        // when
//        mobileApiResourceProvider.getMobileApiVersion(context).getLatestRelease();
//
//        // then
//        assertEquals("customUrl", MobileMessagingCore.getApiUri(context));
//    }

//    @Test
//    public void shouldCalculateAppCodeHashInRequest() {
//        // when
//        try {
//            mobileApiResourceProvider.getMobileApiVersion(context).getLatestRelease();
//            fail();
//        } catch (ApiIOException ignored) {
//        }
//        String applicationCodeInHeaders = debugServer.getHeader(CustomApiHeaders.APPLICATION_CODE.getValue());
//
//        // then
//        assertEquals(10, applicationCodeInHeaders.length());
//        assertEquals("69a991b7f4", applicationCodeInHeaders);
//        assertEquals("69a991b7f4", MobileMessagingCore.getApplicationCodeHash(context, "TestApplicationCode"));
//    }

//    @Test
//    public void shouldForwardCustomHeadersInRequest() {
//        // when
//        try {
//            mobileApiResourceProvider.getMobileApiVersion(context).getLatestRelease();
//            fail();
//        } catch (ApiIOException ignored) {
//        }
//
//        // then
//        assertEquals("69a991b7f4", debugServer.getHeader(CustomApiHeaders.APPLICATION_CODE.getValue()));
//        assertEquals("false", debugServer.getHeader(CustomApiHeaders.FOREGROUND.getValue()));
//        assertEquals(myDeviceRegId, debugServer.getHeader(CustomApiHeaders.PUSH_REGISTRATION_ID.getValue()));
//        assertEquals("UniversalInstallationId", debugServer.getHeader(CustomApiHeaders.INSTALLATION_ID.getValue()));
//    }

    @Test
    public void shouldNotResetBaseUrlOnError() {
        // given
        MobileMessagingCore.setApiUri(context, "http://customurl");

        // when
        try {
            mobileApiResourceProvider.getMobileApiVersion(context).getLatestRelease();
            fail();
        } catch (ApiIOException ignored) {
        }

        // then
        assertEquals("http://customurl", MobileMessagingCore.getApiUri(context));
    }

//    @Test
//    public void shouldUseNewUrlForSecondRequest() {
//        // given
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null, new HashMap<String, String>() {{
//            put(CustomApiHeaders.NEW_BASE_URL.getValue(), "http://customurl");
//        }});
//        MobileApiVersion givenMobileApiVersion = mobileApiResourceProvider.getMobileApiVersion(context);
//
//        // when
//        givenMobileApiVersion.getLatestRelease();
//        try {
//            givenMobileApiVersion.getLatestRelease();
//            fail();
//        } catch (ApiIOException e) {
//            assertTrue(e.getCause() instanceof UnknownHostException);
//            assertTrue(e.getCause().getMessage().contains("customurl"));
//        }
//    }

    @Test
    public void shouldReplaceNotSupportedChars() {
        Map<Integer, String> unsupportedCharCodes = new HashMap<Integer, String>();
        unsupportedCharCodes.put(0x09, "&x09");
        unsupportedCharCodes.put(0x0a, "&x0a");
        unsupportedCharCodes.put(0x0b, "&x0b");
        unsupportedCharCodes.put(0x0c, "&x0c");
        unsupportedCharCodes.put(0x0d, "&x0d");
        unsupportedCharCodes.put(0x11, "&x11");
        unsupportedCharCodes.put(0x12, "&x12");
        unsupportedCharCodes.put(0x13, "&x13");
        unsupportedCharCodes.put(0x14, "&x14");

        for (int charCode : unsupportedCharCodes.keySet()) {
            char unsupported = (char) charCode;
            String test = "someTest" + unsupported + "testEnd";
            String should = "someTesttestEnd";
            String result = UserAgentAdditions.removeNotSupportedChars(test);
            assertEquals(result, should);
        }
    }

    @Test
    public void shouldHandleNewBaseUrlHeaderCaseInsensitiveMixedCase() {
        // Test with mixed case "New-Base-URL" (HTTP/1.1 style)
        MobileMessagingCore.setApiUri(context, "https://initial1.com");
        String initialUrl = MobileMessagingCore.getApiUri(context);
        assertEquals("https://initial1.com", initialUrl);

        MobileApiResourceProvider provider = new MobileApiResourceProvider();
        // Initialize generator by calling a getter method
        provider.getMobileApiVersion(context);
        MobileApiResourceProvider.BaseUrlManager baseUrlManager = provider.new BaseUrlManager(context);

        Map<String, List<String>> headers = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("https://new-mixed.com");
        headers.put("New-Base-URL", values);

        baseUrlManager.beforeResponse(200, headers);

        assertEquals("https://new-mixed.com", MobileMessagingCore.getApiUri(context));
    }

    @Test
    public void shouldHandleNewBaseUrlHeaderCaseInsensitiveLowercase() {
        // Test with lowercase "new-base-url" (HTTP/2 style)
        MobileMessagingCore.setApiUri(context, "https://initial2.com");
        String initialUrl = MobileMessagingCore.getApiUri(context);
        assertEquals("https://initial2.com", initialUrl);

        MobileApiResourceProvider provider = new MobileApiResourceProvider();
        // Initialize generator by calling a getter method
        provider.getMobileApiVersion(context);
        MobileApiResourceProvider.BaseUrlManager baseUrlManager = provider.new BaseUrlManager(context);

        Map<String, List<String>> headers = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("https://new-lower.com");
        headers.put("new-base-url", values);

        baseUrlManager.beforeResponse(200, headers);

        assertEquals("https://new-lower.com", MobileMessagingCore.getApiUri(context));
    }

    @Test
    public void shouldHandleNewBaseUrlHeaderCaseInsensitiveUppercase() {
        // Test with uppercase "NEW-BASE-URL"
        MobileMessagingCore.setApiUri(context, "https://initial3.com");
        String initialUrl = MobileMessagingCore.getApiUri(context);
        assertEquals("https://initial3.com", initialUrl);

        MobileApiResourceProvider provider = new MobileApiResourceProvider();
        // Initialize generator by calling a getter method
        provider.getMobileApiVersion(context);
        MobileApiResourceProvider.BaseUrlManager baseUrlManager = provider.new BaseUrlManager(context);

        Map<String, List<String>> headers = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("https://new-upper.com");
        headers.put("NEW-BASE-URL", values);

        baseUrlManager.beforeResponse(200, headers);

        assertEquals("https://new-upper.com", MobileMessagingCore.getApiUri(context));
    }

//    @Test
//    public void shouldResetUrlAfterFourFailedRequests() {
//        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);
//
//        MobileApiVersion givenMobileApiVersion = mobileApiResourceProvider.getMobileApiVersion(context);
//
//        for (int i = 0; i < 5; i++) {
//            try {
//                givenMobileApiVersion.getLatestRelease();
//            } catch (ApiIOException ignored) {
//            }
//        }
//
//        assertEquals(MobileMessagingProperty.API_URI.getDefaultValue(), MobileMessagingCore.getApiUri(context));
//    }
}
