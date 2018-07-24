package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

import static org.mockito.Mockito.mock;

/**
 * @author sslavin
 * @since 07/07/16.
 */
public class SeenStatusReporterTest extends MobileMessagingTestCase {

    private SeenStatusReporter seenStatusReporter;
    private Executor executor;
    private MobileApiMessages mobileApiMessages;
    private BatchReporter batchReporter;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        batchReporter = new BatchReporter(300L);
        mobileApiMessages = mock(MobileApiMessages.class);

        MobileMessagingStats stats = mobileMessagingCore.getStats();
        executor = mock(Executor.class);
        seenStatusReporter = new SeenStatusReporter(mobileMessagingCore, stats, executor, broadcaster, mobileApiMessages, batchReporter);
    }

    @Test
    public void test_seenReportBatch() throws Exception {

        String messageIds[] = {"1", "2", "3", "4", "5"};
        for (String messageId : messageIds) {
            mobileMessagingCore.setMessagesSeen(messageId);
            seenStatusReporter.sync();
        }

        Mockito.verify(executor, Mockito.times(1)).execute(Mockito.any(Runnable.class));
        Mockito.verify(executor, Mockito.after(500).atMost(2)).execute(Mockito.any(Runnable.class));
    }
}
