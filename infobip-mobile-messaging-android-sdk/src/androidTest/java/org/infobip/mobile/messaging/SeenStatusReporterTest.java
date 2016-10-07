package org.infobip.mobile.messaging;

import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.mobile.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 07/07/16.
 */
public class SeenStatusReporterTest extends InstrumentationTestCase {

    SeenStatusReporter seenStatusReporter;
    Executor executor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveLong(getInstrumentation().getContext(), MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);

        seenStatusReporter = new SeenStatusReporter();
        executor = Mockito.mock(Executor.class);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_seenReportBatch() throws Exception {

        String messageIds[] = {"1", "2", "3", "4", "5"};
        for (String messageId : messageIds) {
            List<String> ids = new ArrayList<>();
            ids.add(messageId);

            seenStatusReporter.report(getInstrumentation().getContext(), ids.toArray(new String[ids.size()]), new MobileMessagingStats(getInstrumentation().getContext()), executor);
        }

        Mockito.verify(executor, Mockito.after(50).never()).execute(Mockito.any(Runnable.class));
        Mockito.verify(executor, Mockito.after(500).atMost(1)).execute(Mockito.any(Runnable.class));
    }
}
