package org.infobip.mobile.messaging.mobile.common;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

import static org.mockito.Matchers.eq;

/**
 * @author sslavin
 * @since 23/07/2017.
 */

public class MAsyncTaskTest extends MobileMessagingTestCase {

    private MAsyncTask<String, String> asyncTask;
    private final Executor executor = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    };
    private MAsyncTaskTester tester;

    interface MAsyncTaskTester {
        void before();
        String run(String s[]);
        void after(String result);
        void error(Throwable error);
        void error(String s[], Throwable error);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        tester = Mockito.mock(MAsyncTaskTester.class);
        asyncTask = new MAsyncTask<String, String>() {

            @Override
            public void before() {
                tester.before();
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
            public void error(Throwable error) {
                tester.error(error);
            }

            @Override
            public void error(String[] strings, Throwable error) {
                tester.error(strings, error);
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
        Mockito.when(tester.run(Mockito.any(String[].class)))
                .thenReturn(givenResult);

        // When
        asyncTask.execute(executor);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).after(givenResult);
        Mockito.verify(tester, Mockito.never()).error(Mockito.any(Throwable.class));
    }

    @Test
    public void shouldExecuteErrorCallbackOnException() {

        // Given
        RuntimeException givenError = new RuntimeException("Error in background");
        Mockito.when(tester.run(Mockito.any(String[].class)))
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
        Mockito.when(tester.run(Mockito.any(String[].class)))
                .thenThrow(givenError);

        // When
        asyncTask.execute(executor, givenInputs);

        // Then
        Mockito.verify(tester, Mockito.after(100).times(1)).error(eq(givenInputs), eq(givenError));
        Mockito.verify(tester, Mockito.never()).after(Mockito.anyString());
    }
}
