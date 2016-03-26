package org.infobip.mobile.messaging.tasks;

import junit.framework.TestCase;
import org.infobip.mobile.messaging.Configuration;
import org.infobip.mobile.messaging.api.ApiClient;
import org.infobip.mobile.messaging.api.HttpMethod;
import org.infobip.mobile.messaging.api.model.ApiResult;

import java.io.IOException;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

/**
 * @author mstipanov
 * @since 03.03.2016.
 */
public class CreateRegistrationTaskTest extends TestCase {
    private static final String API_URL = "API_URL";

    private ApiClient apiClient;
    private CreateRegistrationTask task;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        apiClient = mock(ApiClient.class);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getApiUri()).thenReturn(API_URL);
        task = spy(new CreateRegistrationTask(configuration, apiClient));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_success() throws Exception {
        CreateRegistrationTask.CreateRegistrationParams params = new CreateRegistrationTask.CreateRegistrationParams("A", "B");

        CreateRegistrationTask.CreateRegistrationResponse response = new CreateRegistrationTask.CreateRegistrationResponse();
        ApiResult<CreateRegistrationTask.CreateRegistrationResponse> result = new ApiResult<>(200, "OK", response);
        when(apiClient.execute(HttpMethod.POST, API_URL, params, null, null, CreateRegistrationTask.CreateRegistrationResponse.class)).thenReturn(result);

        task.execute(params);

        verify(apiClient, timeout(5000).times(1)).execute(eq(HttpMethod.POST), eq(API_URL + "/mobile/1/registration"), eq(params), isNull(), isNull(), eq(CreateRegistrationTask.CreateRegistrationResponse.class));
        verify(task, timeout(5000).times(1)).onSuccess(response);
    }

    public void test_exception() throws Exception {
        CreateRegistrationTask.CreateRegistrationParams params = new CreateRegistrationTask.CreateRegistrationParams("A", "B");

        when(apiClient.execute(HttpMethod.POST, API_URL, params, null, null, CreateRegistrationTask.CreateRegistrationResponse.class)).thenThrow(IOException.class);

        task.execute(params);

//        verify(task, timeout(5000).times(1)).onError(notNull(Exception.class));
    }
}