package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author sslavin
 * @since 07/07/16.
 */
public class BatchReporterTest extends MobileMessagingTestCase {

    private BatchReporter batchReporter;
    private Runnable runnable;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        batchReporter = new BatchReporter(50L);
        runnable = Mockito.mock(Runnable.class);
    }

    @Test
    public void test_scheduleMultipleRunOne() throws Exception {

        for (int i = 0; i < 5; i++) {
            batchReporter.put(runnable);
        }

        Mockito.verify(runnable, Mockito.atMost(1)).run();
    }

    @Test
    public void test_scheduleMultipleRunMultiple() throws Exception {

        for (int i = 0; i < 5; i++) {
            batchReporter.put(runnable);
            Thread.sleep(500);
        }

        Mockito.verify(runnable, Mockito.times(5)).run();
    }

    @Test
    public void test_firstRunnableInvokedRightAway() throws Exception {

        batchReporter = new BatchReporter(100L);
        for (int i = 0; i < 5; i++) {
            batchReporter.put(runnable);
        }

        Mockito.verify(runnable, Mockito.times(1)).run();
        Mockito.verify(runnable, Mockito.after(200).times(2)).run();
    }
}
