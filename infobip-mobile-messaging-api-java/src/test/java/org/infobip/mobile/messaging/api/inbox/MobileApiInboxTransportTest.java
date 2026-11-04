/*
 * MobileApiInboxTransportTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.inbox;

import com.sun.net.httpserver.HttpServer;

import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.ApiBackendException;
import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;
import org.infobip.mobile.messaging.api.support.Generator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/** Tests the actual HTTP client against a local server, without contacting the backend. */
public class MobileApiInboxTransportTest {
    private HttpServer server;
    private MobileApiInbox api;
    private volatile URI requestUri;
    private volatile String requestMethod;
    private volatile String authorization;
    private volatile int status = 200;
    private volatile boolean chunked;
    private volatile String contentType = "application/json";
    private volatile String response = "{\"messages\":[],\"countTotal\":0,\"countUnread\":0,\"nextCursor\":\"next+/=\"}";

    @Before
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestUri = exchange.getRequestURI();
            requestMethod = exchange.getRequestMethod();
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            boolean noBody = status == 204 || (bytes.length == 0 && !chunked);
            exchange.sendResponseHeaders(status, noBody ? -1 : chunked ? 0 : bytes.length);
            if (!noBody) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        api = new Generator.Builder()
                .withBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/")
                .build().create(MobileApiInbox.class);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void cursorIsEncodedAndNextCursorIsDeserialized() {
        String cursor = "opaque+/=& cursor";
        FetchInboxResponse inbox = api.fetchInboxPage("user", "Bearer jwt", "100", "200", "offers & news", 20, cursor);

        assertEquals("GET", requestMethod);
        assertEquals("/mobile/1/user/user/inbox/gcm/messages", requestUri.getPath());
        assertEquals("Bearer jwt", authorization);
        assertEquals(Arrays.asList(cursor), queryValues("cursor"));
        assertEquals(Arrays.asList("offers & news"), queryValues("messageTopic"));
        assertEquals(Arrays.asList("100"), queryValues("dateTimeFrom"));
        assertEquals(Arrays.asList("200"), queryValues("dateTimeTo"));
        assertEquals(Arrays.asList("20"), queryValues("limit"));
        assertEquals("next+/=", inbox.getNextCursor());
    }

    @Test
    public void cloudSpecificFirstPageOmitsNullCursor() {
        api.fetchInboxPage("user", "App code", null, null, null, 20, null, "hms");

        assertEquals("/mobile/1/user/user/inbox/hms/messages", requestUri.getPath());
        assertEquals("App code", authorization);
        assertTrue(queryValues("cursor").isEmpty());
    }

    @Test
    public void deleteUsesRepeatedEncodedMessageIdsAndAcceptsNoContent() {
        status = 204;
        api.deleteInboxMessages("user", "Bearer jwt", new String[]{"first+/=", "second & id"});

        assertEquals("DELETE", requestMethod);
        assertEquals("/mobile/1/user/user/inbox/messages", requestUri.getPath());
        assertEquals("Bearer jwt", authorization);
        assertEquals(Arrays.asList("first+/=", "second & id"), queryValues("messageId"));
    }

    @Test
    public void deletePropagatesBackendErrors() {
        status = 400;
        response = "{\"requestError\":{\"serviceException\":{\"messageId\":\"INVALID_IDS\",\"text\":\"Invalid IDs\"}}}";
        try {
            api.deleteInboxMessages("user", "App code", new String[]{"id"});
            fail("Expected a backend error");
        } catch (ApiException error) {
            assertEquals("INVALID_IDS", error.getCode());
            assertEquals("Invalid IDs", error.getMessage());
            assertEquals(Integer.valueOf(400), error.getHttpStatusCode());
        }
        assertEquals("App code", authorization);
    }

    @Test
    public void chunkedInvalidCursorPreservesServiceErrorAndStatus() {
        status = 400;
        chunked = true;
        response = "{\"requestError\":{\"serviceException\":{\"messageId\":\"INVALID_CURSOR\",\"text\":\"Cursor does not match filters\"}}}";

        ApiIOException error = fetchError();

        assertTrue(error instanceof ApiException);
        assertEquals("INVALID_CURSOR", error.getCode());
        assertEquals("Cursor does not match filters", error.getMessage());
        assertEquals(Integer.valueOf(400), error.getHttpStatusCode());
    }

    @Test
    public void chunkedDeleteErrorPreservesServiceErrorAndStatus() {
        status = 403;
        chunked = true;
        response = "{\"requestError\":{\"serviceException\":{\"messageId\":\"DELETE_DENIED\",\"text\":\"Deletion denied\"}}}";

        ApiIOException error = deleteError();

        assertTrue(error instanceof ApiException);
        assertEquals("DELETE_DENIED", error.getCode());
        assertEquals("Deletion denied", error.getMessage());
        assertEquals(Integer.valueOf(403), error.getHttpStatusCode());
    }

    @Test
    public void emptyClientErrorsRetainStatusForFetchAndDelete() {
        response = "";
        for (int code : new int[]{400, 401, 403, 404, 429}) {
            status = code;
            assertHttpFallback(fetchError(), code);
            assertHttpFallback(deleteError(), code);
        }
    }

    @Test
    public void emptyServerErrorRetainsStatusAndBackendExceptionType() {
        status = 503;
        response = "";
        assertHttpFallback(fetchError(), 503);
        assertHttpFallback(deleteError(), 503);
    }

    @Test
    public void htmlGatewayErrorIsNotConvertedToIoError() {
        status = 502;
        chunked = true;
        contentType = "text/html";
        response = "<html><body>Bad Gateway</body></html>";
        assertHttpFallback(fetchError(), 502);
        assertHttpFallback(deleteError(), 502);
    }

    @Test
    public void malformedJsonRetainsStatus() {
        status = 400;
        response = "{\"requestError\":";
        ApiIOException error = fetchError();
        assertHttpFallback(error, 400);
        assertNotNull(error.getCause());
    }

    @Test
    public void nonstandardJsonRetainsStatus() {
        status = 503;
        response = "{\"error\":\"Temporarily unavailable\"}";
        assertHttpFallback(fetchError(), 503);
    }

    @Test
    public void nullAndIncompleteErrorEnvelopesHaveUsefulFallbacks() {
        status = 400;
        for (String body : new String[]{"null", "{}", "{\"requestError\":null}",
                "{\"requestError\":{}}", "{\"requestError\":{\"serviceException\":{}}}"}) {
            response = body;
            assertHttpFallback(fetchError(), 400);
        }
    }

    @Test
    public void missingServiceTextDoesNotDiscardTheServiceCode() {
        status = 400;
        response = "{\"requestError\":{\"serviceException\":{\"messageId\":\"INVALID_CURSOR\"}}}";
        ApiIOException error = fetchError();
        assertEquals("INVALID_CURSOR", error.getCode());
        assertEquals(Integer.valueOf(400), error.getHttpStatusCode());
        assertTrue(error.getMessage().contains("HTTP 400"));
    }

    @Test
    public void missingServiceCodeDoesNotDiscardTheServiceText() {
        status = 403;
        response = "{\"requestError\":{\"serviceException\":{\"text\":\"Access denied\"}}}";
        ApiIOException error = fetchError();
        assertEquals("-3", error.getCode());
        assertEquals("Access denied", error.getMessage());
        assertEquals(Integer.valueOf(403), error.getHttpStatusCode());
    }

    @Test
    public void serverServiceErrorsRetainCodeMessageAndStatus() {
        status = 500;
        chunked = true;
        response = "{\"requestError\":{\"serviceException\":{\"messageId\":\"SERVICE_FAILURE\",\"text\":\"Service failed\"}}}";
        ApiIOException error = fetchError();
        assertTrue(error instanceof ApiBackendException);
        assertEquals("SERVICE_FAILURE", error.getCode());
        assertEquals("Service failed", error.getMessage());
        assertEquals(Integer.valueOf(500), error.getHttpStatusCode());
    }

    @Test
    public void successfulChunkedFetchStillDeserializesNormally() {
        chunked = true;
        FetchInboxResponse inbox = api.fetchInboxPage("user", "App code", null, null, null, 20, null);
        assertEquals("next+/=", inbox.getNextCursor());
        assertTrue(inbox.getMessages().isEmpty());
    }

    @Test
    public void serviceErrorInSuccessfulResponseRetainsContentAndStatus() {
        response = "{\"messages\":[],\"countTotal\":7,\"requestError\":{\"serviceException\":{\"messageId\":\"PARTIAL_ERROR\",\"text\":\"Partial error\"}}}";
        ApiIOException error = fetchError();
        assertTrue(error instanceof ApiBackendExceptionWithContent);
        assertEquals("PARTIAL_ERROR", error.getCode());
        assertEquals(Integer.valueOf(200), error.getHttpStatusCode());
        FetchInboxResponse inbox = (FetchInboxResponse) ((ApiBackendExceptionWithContent) error).getContent();
        assertEquals(7, inbox.getCountTotal());
    }

    private ApiIOException fetchError() {
        return expectApiError(() -> api.fetchInboxPage("user", "App code", null, null, null, 20, "cursor"));
    }

    private ApiIOException deleteError() {
        return expectApiError(() -> api.deleteInboxMessages("user", "App code", new String[]{"id"}));
    }

    private ApiIOException expectApiError(Runnable request) {
        try {
            request.run();
            throw new AssertionError("Expected an API error");
        } catch (ApiIOException error) {
            return error;
        }
    }

    private void assertHttpFallback(ApiIOException error, int expectedStatus) {
        assertEquals(Integer.valueOf(expectedStatus), error.getHttpStatusCode());
        assertEquals(expectedStatus >= 500 ? "-2" : "-3", error.getCode());
        assertTrue(expectedStatus >= 500 ? error instanceof ApiBackendException : error instanceof ApiException);
        assertTrue(error.getMessage().contains("HTTP " + expectedStatus));
    }

    @Test
    public void allInboxRoutesEncodeExternalUserIdLikeIos() {
        assertEncodedUserIdAcrossInboxRequests("a b/c?d#e%f@x.com", "a%20b%2Fc%3Fd%23e%25f@x.com");
    }

    @Test
    public void allInboxRoutesEncodeSemicolonsInUserIds() {
        assertEncodedUserIdAcrossInboxRequests("alice;segment", "alice%3Bsegment");
        assertEncodedUserIdAcrossInboxRequests("alice;segment=value", "alice%3Bsegment=value");
    }

    @Test
    public void literalPercentEscapesAndSemicolonsRemainDistinctIds() {
        assertEncodedUserIdAcrossInboxRequests("alice%3Bsegment", "alice%253Bsegment");
        assertEncodedUserIdAcrossInboxRequests("alice;segment", "alice%3Bsegment");
    }

    @Test
    public void allInboxRoutesEncodeUnicodeUserIds() {
        assertEncodedUserIdAcrossInboxRequests("用户/č😀", "%E7%94%A8%E6%88%B7%2F%C4%8D%F0%9F%98%80");
    }

    @Test
    public void literalPercentEscapesAndSlashesRemainDistinctIds() {
        assertEncodedUserIdAcrossInboxRequests("a%2Fb", "a%252Fb");
        assertEncodedUserIdAcrossInboxRequests("a/b", "a%2Fb");
    }

    @Test
    public void pathPlaceholdersInsideUserIdsAreNotSubstituted() {
        assertEncodedUserIdAcrossInboxRequests("{cloudType}/[user]", "%7BcloudType%7D%2F%5Buser%5D");
    }

    @Test
    public void ordinaryAndPlusAddressedUserIdsRemainUnchanged() {
        assertEncodedUserIdAcrossInboxRequests("user+offers@example.com", "user+offers@example.com");
    }

    private void assertEncodedUserIdAcrossInboxRequests(String rawId, String encodedId) {
        status = 200;
        api.fetchInbox(rawId, "App code", null, null, null, null);
        assertUserPath(rawId, encodedId, "/inbox/gcm/messages", "GET");
        assertNull(requestUri.getRawQuery());

        api.fetchInbox(rawId, "App code", null, null, null, null, "hms");
        assertUserPath(rawId, encodedId, "/inbox/hms/messages", "GET");
        assertNull(requestUri.getRawQuery());

        String cursor = " opaque+/=& cursor ";
        api.fetchInboxPage(rawId, "Bearer jwt", null, null, null, 20, cursor);
        assertUserPath(rawId, encodedId, "/inbox/gcm/messages", "GET");
        assertEquals(Arrays.asList(cursor), queryValues("cursor"));
        assertEquals(Arrays.asList("20"), queryValues("limit"));

        api.fetchInboxPage(rawId, "Bearer jwt", null, null, null, 20, cursor, "hms");
        assertUserPath(rawId, encodedId, "/inbox/hms/messages", "GET");
        assertEquals(Arrays.asList(cursor), queryValues("cursor"));

        status = 204;
        api.deleteInboxMessages(rawId, "Bearer jwt", new String[]{"id+/=#", "second & id"});
        assertUserPath(rawId, encodedId, "/inbox/messages", "DELETE");
        assertEquals(Arrays.asList("id+/=#", "second & id"), queryValues("messageId"));
    }

    private void assertUserPath(String rawId, String encodedId, String suffix, String method) {
        assertEquals(method, requestMethod);
        assertEquals("/mobile/1/user/" + encodedId + suffix, requestUri.getRawPath());
        assertEquals("/mobile/1/user/" + rawId + suffix, requestUri.getPath());
        assertNull(requestUri.getRawFragment());
    }

    private List<String> queryValues(String key) {
        List<String> values = new ArrayList<>();
        if (requestUri.getRawQuery() == null) {
            return values;
        }
        for (String parameter : requestUri.getRawQuery().split("&")) {
            String[] parts = parameter.split("=", 2);
            if (key.equals(URLDecoder.decode(parts[0], StandardCharsets.UTF_8))) {
                values.add(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return values;
    }
}
