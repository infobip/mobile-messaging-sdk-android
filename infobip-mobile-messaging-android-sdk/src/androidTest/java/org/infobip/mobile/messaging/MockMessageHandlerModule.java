/*
 * MockMessageHandlerModule.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import android.content.Context;

/**
 * @author tjuric
 * @since 19/09/17.
 */

public class MockMessageHandlerModule implements MessageHandlerModule {

    @Override
    public void init(Context appContext) {

    }

    @Override
    public boolean handleMessage(Message message) {
        return false;
    }

    @Override
    public boolean messageTapped(Message message) {
        return false;
    }

    @Override
    public void applicationInForeground() {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public void depersonalize() {

    }

    @Override
    public void performSyncActions() {

    }
}
