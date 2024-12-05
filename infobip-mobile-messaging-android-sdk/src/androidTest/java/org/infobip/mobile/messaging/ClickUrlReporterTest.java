package org.infobip.mobile.messaging;

import static org.mockito.Mockito.mock;

import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage;
import org.infobip.mobile.messaging.mobileapi.BatchReporter;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.mobileapi.inapp.InAppClickReporter;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

public class ClickUrlReporterTest extends MobileMessagingTestCase {

    private InAppClickReporter inAppClickReporter;
    private Executor executor;
    private BatchReporter batchReporter;
    private MRetryPolicy retryPolicy;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        batchReporter = new BatchReporter(300L);

        RetryPolicyProvider retryPolicyProvider = new RetryPolicyProvider(context);
        retryPolicy = retryPolicyProvider.DEFAULT();

        MobileMessagingStats stats = mobileMessagingCore.getStats();
        executor = mock(Executor.class);
        inAppClickReporter = new InAppClickReporter(mobileMessagingCore, context, stats, executor, broadcaster, batchReporter, retryPolicy);
    }

    @Test
    public void test_clickUrlReportBatch() throws Exception {

        InAppWebViewMessage message = new InAppWebViewMessage();
        message.clickUrl = "https://www.infobip.com";
        message.url = "https://www.infobip.com";
        message.position = InAppWebViewMessage.InAppWebViewPosition.TOP;
        message.type = InAppWebViewMessage.InAppWebViewType.BANNER;

        String temp = StringUtils.concat(message.clickUrl, "banner", StringUtils.COMMA_WITH_SPACE);
        String clickReport = StringUtils.concat(temp, null, StringUtils.COMMA_WITH_SPACE);

        mobileMessagingCore.reportInAppClick(clickReport);
        inAppClickReporter.sync();

        Mockito.verify(executor, Mockito.times(1)).execute(Mockito.any(Runnable.class));
        Mockito.verify(executor, Mockito.after(500).atMost(2)).execute(Mockito.any(Runnable.class));
    }
}
