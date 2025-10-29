/*
 * MobileApiVersionTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.version.MobileApiVersion;
import org.junit.After;
import org.junit.Before;

/**
 * @author sslavin
 * @since 03/10/2016.
 */

public class MobileApiVersionTest {

//    private DebugServer debugServer;
    private MobileApiVersion mobileApiVersion;

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
//        mobileApiVersion = generator.create(MobileApiVersion.class);
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
//                DefaultApiClient.JSON_SERIALIZER.serialize(new LatestReleaseResponse("GCM", "3.2.1", "www")));
//
//        LatestReleaseResponse response = mobileApiVersion.getLatestRelease();
//
//        //inspect http context
//        assertEquals("/mobile/3/version", debugServer.getUri());
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertNull(debugServer.getBody());
//
//        //inspect response
//        assertEquals("GCM", response.getPlatformType());
//        assertEquals("3.2.1", response.getLibraryVersion());
//        assertEquals("www", response.getUpdateUrl());
//    }
}
