package org.infobip.mobile.messaging.platform;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.cloud.MobileMessagingCloudHandler;

import java.util.concurrent.Executor;

public abstract class PlatformTestCase extends TestCase {
    protected void resetSdkVersion(int sdkVersionInt) {
        Platform.reset(sdkVersionInt);
    }

    protected void resetMobileMessagingCloudHandler(MobileMessagingCloudHandler mobileMessagingCloudHandler) {
        Platform.reset(mobileMessagingCloudHandler);
    }

    protected void resetBackgroundExecutor(Executor executor) {
        Platform.reset(executor);
    }
}
