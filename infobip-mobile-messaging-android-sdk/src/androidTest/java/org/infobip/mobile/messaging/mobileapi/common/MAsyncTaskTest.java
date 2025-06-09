package org.infobip.mobile.messaging.mobileapi.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;
import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendBaseException;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendCommunicationExceptionWithContent;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterExceptionWithContent;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 23/07/2017.
 */

public class MAsyncTaskTest extends MobileMessagingTestCase {

    private MAsyncTask<Object, Object> asyncTask;
    private final Executor executor = Runnable::run;
    private IMAsyncTask<Object, Object> tester;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        //noinspection unchecked
        tester = Mockito.mock(IMAsyncTask.class);
        asyncTask = new MAsyncTask<Object, Object>() {

            @Override
            public void before() {
                tester.before();
            }

            @Override
            public boolean shouldCancel() {
                return tester.shouldCancel();
            }

            @Override
            public Object run(Object[] strings) {
                return tester.run(strings);
            }

            @Override
            public void afterBackground(Object o) {
                tester.afterBackground(o);
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

            @Override
            public void cancelled(Object[] objects) {
                tester.cancelled(objects);
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

        Mockito.when(tester.run(Mockito.any())).thenReturn(givenResult);
        Mockito.when(tester.run(Mockito.eq(new String[0]))).thenReturn(givenResult);
        // When
        asyncTask.execute(executor, "inputValue");

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
        Mockito.verify(tester, Mockito.atLeastOnce()).afterBackground(any());
    }

    @Test
    public void shouldExecuteErrorCallbackWithInputsOnException() {
        // Given
        String[] givenInputs = new String[]{"string1", "string2"};
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
    public void shouldMapInvalidPhoneToAppropriateException() {
        // Given
        ApiIOException givenError = new ApiIOException(ApiErrorCode.PHONE_INVALID, "");
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
        ApiIOException givenError = new ApiIOException(ApiErrorCode.REQUEST_FORMAT_INVALID, "");
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
        ApiIOException givenError = new ApiIOException(ApiErrorCode.EMAIL_INVALID, "");
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
        ApiIOException givenError = new ApiIOException(ApiErrorCode.REQUEST_FORMAT_INVALID, "");
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
    public void shouldMapErrorWithContentAndInvalidParameterToAppropriateException() {
        // Given
        String givenContent = "content";
        ApiIOException givenError = new ApiBackendExceptionWithContent(ApiErrorCode.PHONE_INVALID, "", givenContent);
        Mockito.when(tester.run(any(Object[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(any(Object[].class), eqInvalidParamErrorWithContent(givenError, givenContent));
        Mockito.verify(tester, Mockito.never()).after(Mockito.any());
    }

    @Test
    public void shouldExecuteAfterInBackground() {
        // Given
        String givenResult = "result";
        Mockito.when(tester.run(any(String[].class)))
                .thenReturn(givenResult);

        Mockito.when(tester.run(Mockito.any())).thenReturn(givenResult);
        Mockito.when(tester.run(Mockito.eq(new String[0]))).thenReturn(givenResult);
        // When
        asyncTask.execute(executor, "inputValue");

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).afterBackground(givenResult);
    }

    @Test
    public void shouldBeAbleToCancelExecution() {
        // Given
        Mockito.when(tester.shouldCancel()).thenReturn(true);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).before();
        Mockito.verify(tester, Mockito.times(1)).shouldCancel();
        Mockito.verify(tester, Mockito.times(1)).cancelled(any(Object[].class));
        Mockito.verify(tester, Mockito.never()).run(any(Object[].class));
        Mockito.verify(tester, Mockito.never()).after(any(Object.class));
        Mockito.verify(tester, Mockito.never()).afterBackground(any(Object.class));
        Mockito.verify(tester, Mockito.never()).error(any(Throwable.class));
        Mockito.verify(tester, Mockito.never()).error(any(Object[].class), any(Throwable.class));
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
            public Class<?> type() {
                return ArgumentMatcher.super.type();
            }

            @Override
            public boolean matches(Throwable argument) {
                if (!cls.isInstance(argument)) {
                    return false;
                }

                BackendBaseException exception = (BackendBaseException) argument;
                if (!(exception.getCause() instanceof ApiIOException)) {
                    return false;
                }

                ApiIOException ioException = (ApiIOException) exception.getCause();
                boolean isIoExceptionTheSameAsInnerException = ioException.toString().equals(innerException.toString());
                if (!(argument instanceof BackendCommunicationExceptionWithContent) || content == null) {
                    return isIoExceptionTheSameAsInnerException;
                }

                BackendCommunicationExceptionWithContent exceptionWithContent = (BackendCommunicationExceptionWithContent) argument;
                return isIoExceptionTheSameAsInnerException && exceptionWithContent.getContent().equals(content);
            }
        });
    }
    // endregion
}
