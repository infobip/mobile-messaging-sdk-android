/*
 * MobileApiBaseUrlTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.baseurl.MobileApiBaseUrl;
import org.junit.After;
import org.junit.Before;


public class MobileApiBaseUrlTest {

//    private DebugServer debugServer;
    private MobileApiBaseUrl mobileApiBaseUrl;

    @Before
    public void setUp() throws Exception {
//        debugServer = new DebugServer();
//        debugServer.start();
//
//        Properties properties = new Properties();
//        properties.put("api.key", "my_API_key");
//        Generator generator = new Generator.Builder()
//                .withBaseUrl("http://127.0.0.1:" + debugServer.getListeningPort() + "/")
//                .withProperties(properties)
//                .build();
//
//        mobileApiBaseUrl = generator.create(MobileApiBaseUrl.class);
    }

    @After
    public void tearDown() throws Exception {
//        if (null != debugServer) {
//            try {
//                debugServer.stop();
//            } catch (Exception e) {
//                //ignore
//            }
//        }
    }

//    @Test
//    public void create_success() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK,
//                DefaultApiClient.JSON_SERIALIZER.serialize(new BaseUrlResponse("https://newbaseurl.infobip.com")));
//
//        BaseUrlResponse response = mobileApiBaseUrl.getBaseUrl();
//
//        //inspect http context
//        assertEquals("/mobile/1/baseurl", debugServer.getUri());
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
//        assertEquals(0, debugServer.getQueryParametersCount());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertNull(debugServer.getBody());
//
//        //inspect response
//        assertEquals("https://newbaseurl.infobip.com", response.getBaseUrl());
//    }
}

