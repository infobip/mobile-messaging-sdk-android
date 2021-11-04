package org.infobip.mobile.messaging.mobileapi.common;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

/**
 * @author sslavin
 * @since 24/07/2017.
 */

public class MRetryableTaskTest extends MobileMessagingTestCase {

    private MRetryableTask<String, String> retryableTask;
    private final Executor executor = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    };
    private IMAsyncTask<String, String> tester;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        //noinspection unchecked
        tester = Mockito.mock(IMAsyncTask.class);
        retryableTask = new MRetryableTask<String, String>() {

            @Override
            public void before() {
                tester.before();
            }

            @Override
            public boolean shouldCancel() {
                return tester.shouldCancel();
            }

            @Override
            public String run(String[] strings) {
                return tester.run(strings);
            }

            @Override
            public void after(String s) {
                tester.after(s);
            }

            @Override
            public void afterBackground(String s) {
                tester.afterBackground(s);
            }

            @Override
            public void error(Throwable error) {
                tester.error(error);
            }

            @Override
            public void error(String[] strings, Throwable error) {
                tester.error(strings, error);
            }

            @Override
            public void cancelled(String[] strings) {
                tester.cancelled(strings);
            }
        };
    }

    @Test
    public void shouldExecuteRunMultipleTimesOrError() {

        // Given
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(3)
                .withBackoffMultiplier(0)
                .withRetryOn(Throwable.class)
                .build();
        Mockito.when(tester.run(any(String[].class)))
                .thenThrow(new RuntimeException("Error"));


        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(givenRetryPolicy.getMaxRetries() + 1 /* first run */))
                .run(any(String[].class));
    }

    @Test
    public void shouldExecuteRunUntilThereAreExceptions() {

        // Given
        String givenResult = "someResult";
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(3)
                .withBackoffMultiplier(0)
                .withRetryOn(Throwable.class)
                .build();
        Mockito.when(tester.run(any(String[].class)))
                .thenThrow(new RuntimeException("Error"))
                .thenReturn(givenResult);


        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times( 1 /* first run */ + 1 /* retry */))
                .run(any(String[].class));
        Mockito.verify(tester, Mockito.never()).error(any(Throwable.class));
        Mockito.verify(tester, Mockito.never()).error(any(String[].class), any(Throwable.class));
        Mockito.verify(tester, Mockito.times(1)).before();
        Mockito.verify(tester, Mockito.times(1)).after(givenResult);
    }

    @Test
    public void shouldExecuteRunOnceOnSuccess() {

        // Given
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(3)
                .withBackoffMultiplier(0)
                .build();

        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .run(any(String[].class));
    }

    @Test
    public void shouldExecuteBeforeCallbackOnce() {

        // Given
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(3)
                .withBackoffMultiplier(0)
                .build();

        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .before();
    }

    @Test
    public void shouldExecuteAfterCallbackOnce() {

        // Given
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(3)
                .withBackoffMultiplier(0)
                .build();

        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .after(Mockito.anyString());
    }

    @Test
    public void shouldExecuteAfterBackgroundCallbackOnce() {

        // Given
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(3)
                .withBackoffMultiplier(0)
                .build();

        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .afterBackground(Mockito.anyString());
    }

    @Test
    public void shouldExecuteErrorOnce() {

        // Given
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(3)
                .withBackoffMultiplier(0)
                .build();
        String[] givenInputs = {"input1", "input2"};
        RuntimeException givenError = new RuntimeException("Run failed");
        Mockito.when(tester.run(any(String[].class)))
                .thenThrow(givenError);

        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor, givenInputs);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(givenError);
        Mockito.verify(tester, Mockito.after(100).times(1))
                .error(eq(givenInputs), eq(givenError));
    }

    @Test
    public void shouldNotRetryOnUnknownError() {
        // Given
        class DoRetryException extends RuntimeException {}
        class DontRetryException extends RuntimeException {}
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(2)
                .withRetryOn(DoRetryException.class)
                .withBackoffMultiplier(0)
                .build();
        Mockito.when(tester.run(any(String[].class)))
                .thenThrow(new DontRetryException());

        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).run(any(String[].class));
    }

    @Test
    public void shouldBeAbleToCancelExecution() {
        // Given
        Mockito.when(tester.shouldCancel()).thenReturn(true);
        MRetryPolicy givenRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(3)
                .withBackoffMultiplier(0)
                .build();

        // When
        retryableTask
                .retryWith(givenRetryPolicy)
                .execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).before();
        Mockito.verify(tester, Mockito.times(1)).shouldCancel();
        Mockito.verify(tester, Mockito.times(1)).cancelled(any(String[].class));
        Mockito.verify(tester, Mockito.never()).run(any(String[].class));
        Mockito.verify(tester, Mockito.never()).after(anyString());
        Mockito.verify(tester, Mockito.never()).error(any(Throwable.class));
        Mockito.verify(tester, Mockito.never()).error(any(String[].class), any(Throwable.class));
    }
}
