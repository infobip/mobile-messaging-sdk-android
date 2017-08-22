package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.mobile.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author sslavin
 * @since 07/07/16.
 */
public class SeenStatusReporterTest extends MobileMessagingTestCase {

    SeenStatusReporter seenStatusReporter;
    Executor executor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);

        executor = Executors.newSingleThreadExecutor();
        MobileMessagingStats stats = mobileMessagingCore.getStats();
        seenStatusReporter = new SeenStatusReporter(context, mobileMessagingCore, stats, executor, broadcaster);
        executor = Mockito.mock(Executor.class);
    }

    @Test
    public void test_seenReportBatch() throws Exception {

        String messageIds[] = {"1", "2", "3", "4", "5"};
        for (String messageId : messageIds) {
            mobileMessagingCore.setMessagesSeen(messageId);
            seenStatusReporter.sync();
        }

        Mockito.verify(executor, Mockito.after(50).never()).execute(Mockito.any(Runnable.class));
        Mockito.verify(executor, Mockito.after(500).atMost(1)).execute(Mockito.any(Runnable.class));
    }
}
