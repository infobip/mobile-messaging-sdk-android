/*
 * PathEncodingTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support;

import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Path;
import org.infobip.mobile.messaging.api.support.http.Paths;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class PathEncodingTest {
    private RecordingClient client;
    private TestApi api;

    @Before
    public void setUp() {
        Properties properties = new Properties();
        properties.setProperty("raw.id", "a/b%2F");
        Generator generator = new Generator.Builder()
                .withBaseUrl("https://example.com/")
                .withProperties(properties)
                .build();
        client = new RecordingClient();
        generator.setApiClient(client);
        api = generator.create(TestApi.class);
    }

    @Test
    public void parameterEncodingIsOptIn() {
        api.encoded("a b/c?d#e%f@x.com");
        assertEquals("https://example.com/items/a%20b%2Fc%3Fd%23e%25f@x.com", client.uri);
    }

    @Test
    public void unannotatedEncodingPreservesLegacyPathsAndPreEncodedValues() {
        api.legacy("a/b%2Fc");
        assertEquals("https://example.com/items/a/b%2Fc", client.uri);
    }

    @Test
    public void encodedLookingInputIsLiteralDataNotDecodedOrTrusted() {
        api.encoded("a%2Fb");
        assertEquals("https://example.com/items/a%252Fb", client.uri);
    }

    @Test
    public void semicolonIsEncodedInsteadOfBecomingAMatrixParameter() {
        api.encoded("alice;segment");
        assertEquals("https://example.com/items/alice%3Bsegment", client.uri);
    }

    @Test
    public void encodedLookingSemicolonRemainsLiteralData() {
        api.encoded("alice%3Bsegment");
        assertEquals("https://example.com/items/alice%253Bsegment", client.uri);
    }

    @Test
    public void legacyPathSubstitutionStillLeavesSemicolonsUnchanged() {
        api.legacy("alice;segment");
        assertEquals("https://example.com/items/alice;segment", client.uri);
    }

    @Test
    public void unicodeUsesUtf8PercentEncoding() {
        api.encoded("用户/č😀");
        assertEquals("https://example.com/items/%E7%94%A8%E6%88%B7%2F%C4%8D%F0%9F%98%80", client.uri);
    }

    @Test
    public void methodPathEncodesInjectedRawProperty() {
        api.methodPath();
        assertEquals("https://example.com/items/a%2Fb%252F", client.uri);
    }

    @Test
    public void methodPathsApplyEncodingOnlyWhereRequested() {
        api.methodPaths();
        assertEquals("https://example.com/items/a%2Fb%252F/legacy/pre%2Fencoded", client.uri);
    }

    @HttpRequest("items/{id}")
    private interface TestApi {
        @HttpRequest
        void encoded(@Path(name = "id", encode = true) String id);

        @HttpRequest
        void legacy(@Path(name = "id") String id);

        @HttpRequest
        @Path(name = "id", value = "${raw.id}", encode = true)
        void methodPath();

        @HttpRequest("{suffix}")
        @Paths({
                @Path(name = "id", value = "${raw.id}", encode = true),
                @Path(name = "suffix", value = "legacy/pre%2Fencoded")
        })
        void methodPaths();
    }

    private static class RecordingClient extends DefaultApiClient {
        String uri;

        @Override
        public <B, R> R execute(HttpMethod method, String uri, String apiKey, Tuple<String, String> credentials,
                                Map<String, Collection<Object>> queryParams, Map<String, Collection<Object>> headers,
                                B body, Class<R> responseType) {
            this.uri = uri;
            return null;
        }
    }
}
