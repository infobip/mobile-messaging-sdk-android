package org.infobip.mobile.messaging.tasks;

import junit.framework.TestCase;
import org.infobip.mobile.messaging.api.support.http.client.ApiClient;
import org.infobip.mobile.messaging.mobile.registration.UpsertRegistrationTask;

/**
 * TODO implement this test!
 *
 * @author mstipanov
 * @since 03.03.2016.
 */
public class UpsertRegistrationTaskTest extends TestCase {
    private static final String API_URL = "API_URL";

    private ApiClient apiClient;
    private UpsertRegistrationTask task;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
//        apiClient = mock(ApiClient.class);
//        Configuration configuration = mock(Configuration.class);
//        when(configuration.getApiUri()).thenReturn(API_URL);
//        task = spy(new UpsertRegistrationTask(configuration, apiClient));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_success() throws Exception {
//        UpsertRegistrationTask.CreateRegistrationParams params = new UpsertRegistrationTask.CreateRegistrationParams("A", "B");
//
//        UpsertRegistrationTask.CreateRegistrationResponse response = new UpsertRegistrationTask.CreateRegistrationResponse();
//        ApiResult<UpsertRegistrationTask.CreateRegistrationResponse> result = new ApiResult<>(200, "OK", response);
//        when(apiClient.execute(HttpMethod.POST, API_URL, params, null, null, UpsertRegistrationTask.CreateRegistrationResponse.class)).thenReturn(result);
//
//        task.execute(params);
//
//        verify(apiClient, timeout(5000).times(1)).execute(eq(HttpMethod.POST), eq(API_URL + "/mobile/1/registration"), eq(params), isNull(), isNull(), eq(UpsertRegistrationTask.CreateRegistrationResponse.class));
//        verify(task, timeout(5000).times(1)).onSuccess(response);
    }

    public void test_exception() throws Exception {
//        UpsertRegistrationTask.CreateRegistrationParams params = new UpsertRegistrationTask.CreateRegistrationParams("A", "B");
//
//        when(apiClient.execute(HttpMethod.POST, API_URL, params, null, null, UpsertRegistrationTask.CreateRegistrationResponse.class)).thenThrow(IOException.class);
//
//        task.execute(params);

//        verify(task, timeout(5000).times(1)).onError(notNull(Exception.class));
    }
}