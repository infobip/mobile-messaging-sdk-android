package org.infobip.mobile.messaging.api.support.http.client;

import org.apache.commons.codec.binary.Base64;
import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;
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
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient.DEFAULT_CONNECT_TIMEOUT;
import static org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient.DEFAULT_READ_TIMEOUT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public class DefaultApiClientTest {
    private DebugServer debugServer;
    private DefaultApiClient apiClient;

    private RequestInterceptor requestInterceptorMock;
    private ResponsePreProcessor responsePreProcessorMock;

    @Before
    public void setUp() throws Exception {
        requestInterceptorMock = mock(RequestInterceptor.class);
        responsePreProcessorMock = mock(ResponsePreProcessor.class);

        when(requestInterceptorMock.intercept(any(Request.class))).thenAnswer(new Answer<Request>() {
            @Override
            public Request answer(InvocationOnMock invocation) throws Throwable {
                return (Request) invocation.getArguments()[0];
            }
        });

        apiClient = new DefaultApiClient(
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_READ_TIMEOUT,
                null,
                new RequestInterceptor[]{requestInterceptorMock},
                new ResponsePreProcessor[]{responsePreProcessorMock},
                new Logger());

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

    @Test
    public void execute_receivesOK_withRequestErrorAndResponseBody() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, DefaultApiClient.JSON_SERIALIZER.serialize(new SomeApiResponse("1", "Invalid Application ID", 123)));

        try {
            apiClient.execute(HttpMethod.POST, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map(), null, null, SomeApiResponse.class);
            Assert.fail("Expected exception ApiBackendExceptionWithContent is not thrown");
        } catch (ApiBackendExceptionWithContent error) {
            Assert.assertEquals("1", error.getCode());
            Assert.assertEquals("Invalid Application ID", error.getMessage());
            Assert.assertEquals(123, ((SomeApiResponse) error.getContent()).getInternalRegistrationId());
        }
    }

    @Test
    public void execute_withRequestInterceptors_interceptorsShouldReceiveAllData() throws Exception {
        Map<String, Collection<Object>> givenQueryParams = new HashMap<String, Collection<Object>>() {{
            put("param1", new ArrayList<>());
            put("param2", new ArrayList<>());
        }};

        Map<String, Collection<Object>> givenHeaders = new HashMap<String, Collection<Object>>() {{
            put("header1", new ArrayList<>());
            put("header2", new ArrayList<>());
        }};

        Map<String, String> givenBody = new HashMap<String, String>() {{
            put("key", "value");
        }};

        try {
            apiClient.execute(HttpMethod.GET,
                    "http://some_url",
                    "someApiKey",
                    new Tuple<>("user", "pass"),
                    givenQueryParams,
                    givenHeaders,
                    givenBody,
                    Map.class);
        } catch (Exception ignored) {

        }

        verify(requestInterceptorMock, times(1)).intercept(matches(new Request(
                HttpMethod.GET,
                "http://some_url",
                "someApiKey",
                new Tuple<>("user", "pass"),
                givenHeaders,
                givenQueryParams,
                givenBody)));
    }

    @Test
    public void execute_withResponseHeaderInterceptors_interceptorsShouldReceiveAllData() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null, new HashMap<String, String>() {{
            put("header1", "value1");
            put("header2", "value2");
        }});

        apiClient.execute(HttpMethod.POST, "http://127.0.0.1:" + debugServer.getListeningPort(), null, null, MapUtils.map(), MapUtils.map(), null, SomeApiResponse.class);

        verify(responsePreProcessorMock, times(1))
                .beforeResponse(eq(NanoHTTPD.Response.Status.OK.getRequestStatus()), contansAll(new HashMap<String, Collection<Object>>() {{
            put("header1", Collections.<Object>singleton("value1"));
            put("header2", Collections.<Object>singleton("value2"));
        }}));
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

        public SomeApiResponse(String code, String text, int internalRegistrationId) {
            super(code, text);
            this.internalRegistrationId = internalRegistrationId;
        }
    }

    private static Request matches(final Request givenRequest) {
        return argThat(new ArgumentMatcher<Request>() {
            @Override
            public boolean matches(Object argument) {
                Request another = (Request) argument;
                return givenRequest.uri.equalsIgnoreCase(another.uri) &&
                        givenRequest.body.equals(another.body) &&
                        givenRequest.credentials.equals(another.credentials) &&
                        givenRequest.apiKey.equalsIgnoreCase(another.apiKey) &&
                        givenRequest.headers.keySet().equals(another.headers.keySet()) &&
                        givenRequest.queryParams.keySet().equals(another.queryParams.keySet()) &&
                        givenRequest.httpMethod.equals(another.httpMethod);
            }
        });
    }

    private static Map<String, List<String>> contansAll(final Map<String, Collection<Object>> headers) {
        return argThat(new ArgumentMatcher<Map<String, List<String>>>() {
            @Override
            public boolean matches(Object argument) {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> arg = (Map<String, List<String>>) argument;
                for (String key : arg.keySet()) {
                    if (key == null || !headers.containsKey(key)) {
                        continue;
                    }

                    if (!headers.get(key).containsAll(arg.get(key))) {
                        return false;
                    }
                }
                return true;
            }
        });
    }
}