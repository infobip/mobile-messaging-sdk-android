/*
 * MessageHandlerModuleTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author tjuric
 * @since 19/09/17.
 */
@RunWith(AndroidJUnit4.class)
public class MessageHandlerModuleTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = getInstrumentation().getContext();
    }

    @Test
    public void should_return_null_when_invalid_classname_provided() throws InterruptedException {
        // Given
        class UnknownModule implements MessageHandlerModule {
            public void init(Context appContext) {}
            public boolean handleMessage(Message message) {return false;}
            public boolean messageTapped(Message message) {return false;}
            public void applicationInForeground() {}
            public void cleanup() {}
            public void depersonalize() {}
            public void performSyncActions() {}
        }

        // When
        MessageHandlerModule messageHandlerModule = MobileMessagingCore.getInstance(context).getMessageHandlerModule(UnknownModule.class);

        // Then
        assertNull(messageHandlerModule);
    }

    @Test
    public void should_return_module_when_valid_classname_provided() throws InterruptedException {
        // When
        MessageHandlerModule messageHandlerModule = MobileMessagingCore.getInstance(context).getMessageHandlerModule(MockMessageHandlerModule.class);

        // Then
        assertNotNull(messageHandlerModule);
        assertTrue(messageHandlerModule instanceof MockMessageHandlerModule);
    }
}
