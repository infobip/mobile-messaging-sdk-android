/*
 * BaseUrlCheckerTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.baseurl;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.baseurl.BaseUrlResponse;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessagingProperty.BASEURL_CHECK_INTERVAL_HOURS;
import static org.infobip.mobile.messaging.MobileMessagingProperty.BASEURL_CHECK_LAST_TIME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseUrlCheckerTest extends MobileMessagingTestCase {

    private BaseUrlChecker baseUrlChecker;
    private String baseUrl = "https://newbaseurl.infobip.com";
    private Executor executor = Runnable::run;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        baseUrlChecker = new BaseUrlChecker(context, executor, mobileApiBaseUrl);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Mockito.reset(mobileApiBaseUrl);
        PreferenceHelper.remove(context, BASEURL_CHECK_LAST_TIME);
        PreferenceHelper.remove(context, BASEURL_CHECK_INTERVAL_HOURS);
    }

    @Test
    public void shouldGetBaseUrl() {
        when(mobileApiBaseUrl.getBaseUrl()).thenReturn(new BaseUrlResponse(baseUrl));

        baseUrlChecker.sync();

        verify(mobileApiBaseUrl, after(300).times(1)).getBaseUrl();
        String apiUri = MobileMessagingCore.getApiUri(context);
        assertEquals(baseUrl, apiUri);
    }

//    @Test
//    public void shouldNotThrowAndShouldUseDefaultBaseUrlWhenUrlIsAbsent() {
//        when(mobileApiBaseUrl.getBaseUrl()).thenReturn(new BaseUrlResponse());
//
//        baseUrlChecker.sync();
//
//        verify(mobileApiBaseUrl, after(300).times(1)).getBaseUrl();
//        String apiUri = MobileMessagingCore.getApiUri(context);
//        assertTrue(apiUri.startsWith("http://127.0.0.1:"));
//    }

//    @Test
//    public void baseUrlSyncShouldDoNothingWhenErrorIsReturned() {
//        doThrow(new ApiIOException("some error code", "some error message")).when(mobileApiBaseUrl).getBaseUrl();
//
//        baseUrlChecker.sync();
//
//        verify(mobileApiBaseUrl, after(300).times(1)).getBaseUrl();
//        String apiUri = MobileMessagingCore.getApiUri(context);
//        assertTrue(apiUri.startsWith("http://127.0.0.1:"));
//    }

    @Test
    public void shouldCallBaseUrlSyncOnlyOnceForSeveralAttempts() {
        when(mobileApiBaseUrl.getBaseUrl()).thenReturn(new BaseUrlResponse(baseUrl));

        baseUrlChecker.sync();
        baseUrlChecker.sync();
        baseUrlChecker.sync();

        verify(mobileApiBaseUrl, after(300).times(1)).getBaseUrl();
        assertEquals(baseUrl, MobileMessagingCore.getApiUri(context));

        baseUrlChecker.sync();
        baseUrlChecker.sync();

        verify(mobileApiBaseUrl, after(300).times(1)).getBaseUrl();
        assertEquals(baseUrl, MobileMessagingCore.getApiUri(context));
    }

    @Test
    public void shouldCallBaseUrlSyncIfNoHoursIntervalIsSetUpAndSyncIsNotInProgress() throws InterruptedException {
        PreferenceHelper.saveInt(context, BASEURL_CHECK_INTERVAL_HOURS, 0);

        when(mobileApiBaseUrl.getBaseUrl()).thenReturn(new BaseUrlResponse(baseUrl));

        baseUrlChecker.sync();
        Thread.sleep(500);
        baseUrlChecker.sync();
        Thread.sleep(500);

        verify(mobileApiBaseUrl, Mockito.times(2)).getBaseUrl();
        assertEquals(baseUrl, MobileMessagingCore.getApiUri(context));
        baseUrlChecker.sync();
        Thread.sleep(500);
        baseUrlChecker.sync();
        Thread.sleep(500);

        verify(mobileApiBaseUrl, Mockito.times(4)).getBaseUrl();
        assertEquals(baseUrl, MobileMessagingCore.getApiUri(context));
    }

}
