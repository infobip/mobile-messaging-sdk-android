package org.infobip.mobile.messaging.mobile.common;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;
import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendBaseException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationExceptionWithContent;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterExceptionWithContent;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;

/**
 * @author sslavin
 * @since 23/07/2017.
 */

public class MAsyncTaskTest extends MobileMessagingTestCase {

    private MAsyncTask<Object, Object> asyncTask;
    private final Executor executor = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    };
    private MAsyncTaskTester tester;

    interface MAsyncTaskTester {
        void before();
        String run(Object s[]);
        void after(Object result);
        void error(Throwable error);
        void error(Object s[], Throwable error);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        tester = Mockito.mock(MAsyncTaskTester.class);
        asyncTask = new MAsyncTask<Object, Object>() {

            @Override
            public void before() {
                tester.before();
            }

            @Override
            public String run(Object[] strings) {
                return tester.run(strings);
            }

            @Override
            public void after(Object s) {
                tester.after(s);
            }

            @Override
            public void error(Throwable error) {
                tester.error(error);
            }

            @Override
            public void error(Object[] objects, Throwable error) {
                tester.error(objects, error);
            }
        };
    }

    @Test
    public void shouldExecuteCallbackBefore() {

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .before();
    }

    @Test
    public void shouldExecuteCallbackAfterSuccess() {

        // Given
        String givenResult = "result";
        Mockito.when(tester.run(any(String[].class)))
                .thenReturn(givenResult);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).after(givenResult);
        Mockito.verify(tester, Mockito.never()).error(any(Throwable.class));
    }

    @Test
    public void shouldExecuteErrorCallbackOnException() {

        // Given
        RuntimeException givenError = new RuntimeException("Error in background");
        Mockito.when(tester.run(any(String[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).error(givenError);
        Mockito.verify(tester, Mockito.never()).after(Mockito.anyString());
    }

    @Test
    public void shouldExecuteErrorCallbackWithInputsOnException() {
        // Given
        String givenInputs[] = {"string1", "string2"};
        RuntimeException givenError = new RuntimeException("Error in background");
        Mockito.when(tester.run(any(String[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor, givenInputs);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).error(eq(givenInputs), eq(givenError));
        Mockito.verify(tester, Mockito.never()).after(Mockito.anyString());
    }

    @Test
    public void shouldMapInvalidMsisdnToAppropriateException() {
        // Given
        ApiIOException givenError = new ApiIOException(ApiErrorCode.INVALID_MSISDN_FORMAT, "");
        Mockito.when(tester.run(any(Object[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(any(Object[].class), eqInvalidParamErrorWith(givenError));
        Mockito.verify(tester, Mockito.never()).after(Mockito.any());
    }

    @Test
    public void shouldMapInvalidCustomValueToAppropriateException() {
        // Given
        ApiIOException givenError = new ApiIOException(ApiErrorCode.INVALID_VALUE, "");
        Mockito.when(tester.run(any(Object[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(any(Object[].class), eqInvalidParamErrorWith(givenError));
        Mockito.verify(tester, Mockito.never()).after(Mockito.any());
    }

    @Test
    public void shouldMapInvalidEmailToAppropriateException() {
        // Given
        ApiIOException givenError = new ApiIOException(ApiErrorCode.INVALID_EMAIL_FORMAT, "");
        Mockito.when(tester.run(any(Object[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(any(Object[].class), eqInvalidParamErrorWith(givenError));
        Mockito.verify(tester, Mockito.never()).after(Mockito.any());
    }

    @Test
    public void shouldMapInvalidBirthdateToAppropriateException() {
        // Given
        ApiIOException givenError = new ApiIOException(ApiErrorCode.INVALID_BIRTHDATE_FORMAT, "");
        Mockito.when(tester.run(any(Object[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(any(Object[].class), eqInvalidParamErrorWith(givenError));
        Mockito.verify(tester, Mockito.never()).after(Mockito.any());
    }

    @Test
    public void shouldMapErrorWithContentToAppropriateException() {
        // Given
        String givenContent = "content";
        ApiIOException givenError = new ApiBackendExceptionWithContent(ApiErrorCode.CONTACT_SERVICE_ERROR, "", givenContent);
        Mockito.when(tester.run(any(Object[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(any(Object[].class), eqErrorWithContent(givenError, givenContent));
        Mockito.verify(tester, Mockito.never()).after(Mockito.any());
    }

    @Test
    public void shouldMapErrorWithContentAndInvalidParameterToAppropriateException() {
        // Given
        String givenContent = "content";
        ApiIOException givenError = new ApiBackendExceptionWithContent(ApiErrorCode.INVALID_MSISDN_FORMAT, "", givenContent);
        Mockito.when(tester.run(any(Object[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(any(Object[].class), eqInvalidParamErrorWithContent(givenError, givenContent));
        Mockito.verify(tester, Mockito.never()).after(Mockito.any());
    }

    // region private methods

    @NonNull
    private Throwable eqInvalidParamErrorWith(final ApiIOException innerException) {
        return eqBackendError(BackendInvalidParameterException.class, innerException, null);
    }

    @NonNull
    private Throwable eqInvalidParamErrorWithContent(final ApiIOException innerException, final Object content) {
        return eqBackendError(BackendInvalidParameterExceptionWithContent.class, innerException, content);
    }

    @NonNull
    private Throwable eqErrorWithContent(final ApiIOException innerException, final Object content) {
        return eqBackendError(BackendCommunicationExceptionWithContent.class, innerException, content);
    }

    @NonNull
    private Throwable eqBackendError(final Class<? extends BackendBaseException> cls, final ApiIOException innerException, final Object content) {
        return argThat(new ArgumentMatcher<Throwable>() {
            @Override
            public boolean matches(Object argument) {
                if (!cls.isInstance(argument)) {
                    return false;
                }

                BackendBaseException exception = (BackendBaseException) argument;
                if (!(exception.getCause() instanceof ApiIOException)) {
                    return false;
                }

                ApiIOException ioException = (ApiIOException) exception.getCause();
                if (!(argument instanceof BackendCommunicationExceptionWithContent) || content == null) {
                    return ioException.toString().equals(innerException.toString());
                }

                BackendCommunicationExceptionWithContent exceptionWithContent = (BackendCommunicationExceptionWithContent) argument;
                return ioException.toString().equals(innerException.toString()) && exceptionWithContent.getContent().equals(content);
            }
        });
    }
    // endregion
}
