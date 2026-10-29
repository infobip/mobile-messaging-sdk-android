/*
 * MobileMessagingBuilderTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import com.google.firebase.FirebaseOptions;

import org.infobip.mobile.messaging.cloud.MobileMessagingCloudHandler;
import org.infobip.mobile.messaging.platform.Lazy;
import org.infobip.mobile.messaging.platform.Platform;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class MobileMessagingBuilderTest extends MobileMessagingTestCase {

    private Application application;
    private FirebaseOptions firebaseOptions;
    private Lazy<MobileMessagingCore, Context> originalMobileMessagingCore;
    private Lazy<MobileMessagingCloudHandler, Context> originalMobileMessagingCloudHandler;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        application = (Application) context.getApplicationContext();
        firebaseOptions = new FirebaseOptions.Builder().setProjectId("project_id").setApiKey("api_key").setApplicationId("application_id").build();
        // avoid forced token reset on init, it's not what is tested here
        PreferenceHelper.saveString(context, MobileMessagingProperty.REPORTED_PUSH_SERVICE_TYPE, Platform.usedPushServiceType.name());

        // build() replaces platform singletons, restore them afterwards so other tests are not affected
        originalMobileMessagingCore = Platform.mobileMessagingCore;
        originalMobileMessagingCloudHandler = Platform.mobileMessagingCloudHandler;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        Platform.mobileMessagingCore = originalMobileMessagingCore;
        Platform.mobileMessagingCloudHandler = originalMobileMessagingCloudHandler;
        super.tearDown();
    }

    @Test
    public void test_app_managed_fcm_token_lifecycle_is_disabled_by_default() {
        MobileMessagingCore core = build(givenBuilder());

        assertFalse(core.isAppManagedFcmTokenLifecycle());
    }

    @Test
    public void test_app_managed_fcm_token_lifecycle_is_enabled_via_builder() {
        MobileMessagingCore core = build(givenBuilder().withAppManagedFcmTokenLifecycle(true));

        assertTrue(core.isAppManagedFcmTokenLifecycle());
        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.APP_MANAGED_FCM_TOKEN_LIFECYCLE));
    }

    @Test
    public void test_app_managed_fcm_token_lifecycle_is_disabled_when_omitted_on_subsequent_build() {
        build(givenBuilder().withAppManagedFcmTokenLifecycle(true));

        MobileMessagingCore core = build(givenBuilder());

        assertFalse(core.isAppManagedFcmTokenLifecycle());
    }

    private MobileMessaging.Builder givenBuilder() {
        return new MobileMessaging.Builder(application)
                .withApplicationCode("TestApplicationCode")
                .withFirebaseOptions(firebaseOptions);
    }

    // SDK is initialized on the main thread in apps (Application.onCreate), build() relies on it
    private MobileMessagingCore build(MobileMessaging.Builder builder) {
        AtomicReference<MobileMessaging> mobileMessaging = new AtomicReference<>();
        getInstrumentation().runOnMainSync(() -> mobileMessaging.set(builder.build()));
        return (MobileMessagingCore) mobileMessaging.get();
    }
}
