package org.infobip.mobile.messaging.api.support.http.client;

import fi.iki.elonen.NanoHTTPD;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.apache.commons.codec.binary.Base64;
import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.api.support.http.client.model.ApiError;
import org.infobip.mobile.messaging.api.support.http.client.model.ApiResponse;
import org.infobip.mobile.messaging.api.support.http.client.model.ApiServiceException;
import org.infobip.mobile.messaging.api.support.util.MapUtils;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public class DefaultApiClientTest {
    private DebugServer debugServer;
    private DefaultApiClient apiClient;

    @Before
    public void setUp() throws Exception {
        apiClient = new DefaultApiClient();
        debugServer = new DebugServer();
        debugServer.start();
    }

    @After
    public void tearDown() throws Exception {
        debugServer.stop();
    }

    @Test(expected = ApiException.class)
    public void execute_withQueryParams_noHeaders_noBody_receivesError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.UNAUTHORIZED,
                DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse(
                        new ApiError(new ApiServiceException("1", "Invalid Application ID")))));

        apiClient.execute(HttpMethod.GET, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), null, null, ApiResponse.class);
    }

    @Test
    public void execute_withQueryParams_noHeaders_noBody_receivesOK() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, DefaultApiClient.JSON_SERIALIZER.serialize(new SomeApiResponse(11)));

        SomeApiResponse result = apiClient.execute(HttpMethod.GET, "http://127.0.0.1:" + debugServer.getListeningPort(), "12345", null, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), null, null, SomeApiResponse.class);

        Assert.assertEquals(11, result.getInternalRegistrationId());
        Assert.assertEquals(1, debugServer.getRequestCount());
        Assert.assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
        Assert.assertEquals("/", debugServer.getUri());
        Assert.assertEquals("xyz", debugServer.getQueryParameter("applicationId"));
        Assert.assertEquals("1234", debugServer.getQueryParameter("currentRegistrationId"));
        Assert.assertNull(debugServer.getQueryParameter("oldRegistrationId"));
        Assert.assertEquals("App 12345", debugServer.getHeader("Authorization"));
    }

    @Test(expected = ApiException.class)
    public void execute_withQueryParams_noHeaders_withBody_receivesError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.UNAUTHORIZED,
                DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse(
                        new ApiError(new ApiServiceException("1", "Invalid Application ID")))));

        apiClient.execute(HttpMethod.POST, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), null, new SomeApiRequest("Test"), ApiResponse.class);
    }

    @Test
    public void execute_withQueryParams_noHeaders_withBody_receivesOK() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, DefaultApiClient.JSON_SERIALIZER.serialize(new SomeApiResponse(11)));

        SomeApiResponse result = apiClient.execute(HttpMethod.POST, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), null, new SomeApiRequest("Test"), SomeApiResponse.class);

        Assert.assertEquals(11, result.getInternalRegistrationId());
        Assert.assertEquals(1, debugServer.getRequestCount());
        Assert.assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        Assert.assertEquals("/", debugServer.getUri());
        Assert.assertEquals("xyz", debugServer.getQueryParameter("applicationId"));
        Assert.assertEquals("1234", debugServer.getQueryParameter("currentRegistrationId"));
        Assert.assertNull(debugServer.getQueryParameter("oldRegistrationId"));
        Assert.assertEquals("{\"name\":\"Test\"}", debugServer.getBody());
    }

    @Test(expected = ApiException.class)
    public void execute_withQueryParams_withHeaders_noBody_receivesError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.UNAUTHORIZED,
                DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse(
                        new ApiError(new ApiServiceException("1", "Invalid Application ID")))));

        apiClient.execute(HttpMethod.GET, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), MapUtils.map("X-Test-1", "test1", "X-Test-2", "test2"), null, ApiResponse.class);
    }

    @Test
    public void execute_withQueryParams_withHeaders_noBody_receivesOK() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, DefaultApiClient.JSON_SERIALIZER.serialize(new SomeApiResponse(11)));

        SomeApiResponse result = apiClient.execute(HttpMethod.GET, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), MapUtils.map("X-Test-1", "test1", "X-Test-2", "test2"), null, SomeApiResponse.class);

        Assert.assertEquals(11, result.getInternalRegistrationId());
        Assert.assertEquals(1, debugServer.getRequestCount());
        Assert.assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
        Assert.assertEquals("/", debugServer.getUri());
        Assert.assertEquals("xyz", debugServer.getQueryParameter("applicationId"));
        Assert.assertEquals("1234", debugServer.getQueryParameter("currentRegistrationId"));
        Assert.assertNull(debugServer.getQueryParameter("oldRegistrationId"));
        Assert.assertEquals("test1", debugServer.getHeader("X-Test-1"));
        Assert.assertEquals("test2", debugServer.getHeader("X-Test-2"));
    }

    @Test(expected = ApiException.class)
    public void execute_withQueryParams_withHeaders_withBody_receivesError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.UNAUTHORIZED,
                DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse(
                        new ApiError(new ApiServiceException("1", "Invalid Application ID")))));

        apiClient.execute(HttpMethod.POST, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), MapUtils.map("X-Test-1", "test1", "X-Test-2", "test2"), new SomeApiRequest("Test"), ApiResponse.class);
    }

    @Test
    public void execute_withQueryParams_withHeaders_withBody_receivesOK() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, DefaultApiClient.JSON_SERIALIZER.serialize(new SomeApiResponse(11)));

        SomeApiResponse result = apiClient.execute(HttpMethod.POST, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), MapUtils.map("X-Test-1", "test1", "X-Test-2", "test2"), new SomeApiRequest("Test"), SomeApiResponse.class);

        Assert.assertEquals(11, result.getInternalRegistrationId());
        Assert.assertEquals(1, debugServer.getRequestCount());
        Assert.assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        Assert.assertEquals("/", debugServer.getUri());
        Assert.assertEquals("xyz", debugServer.getQueryParameter("applicationId"));
        Assert.assertEquals("1234", debugServer.getQueryParameter("currentRegistrationId"));
        Assert.assertNull(debugServer.getQueryParameter("oldRegistrationId"));
        Assert.assertEquals("{\"name\":\"Test\"}", debugServer.getBody());
        Assert.assertEquals("test1", debugServer.getHeader("X-Test-1"));
        Assert.assertEquals("test2", debugServer.getHeader("X-Test-2"));
    }

    @Test
    public void execute_noQueryParams_noHeaders_noBody_basicAuth_receivesOK() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        String user = "user";
        String password = "password";
        Tuple<String, String> credentials = new Tuple<>(user, password);
        String base64Auth = Base64.encodeBase64String((user + ":" + password).getBytes());

        apiClient.execute(HttpMethod.POST, "http://127.0.0.1:" + debugServer.getListeningPort(), null, credentials, MapUtils.map("applicationId", "xyz", "currentRegistrationId", "1234"), null, null, null);
        Assert.assertEquals(debugServer.getHeader("Authorization"), "Basic " + base64Auth);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SomeApiRequest {
        private String name;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SomeApiResponse extends ApiResponse {
        private int internalRegistrationId;
    }
}