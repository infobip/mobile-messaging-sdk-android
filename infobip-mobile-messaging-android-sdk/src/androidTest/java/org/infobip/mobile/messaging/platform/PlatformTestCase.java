/*
 * PlatformTestCase.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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
