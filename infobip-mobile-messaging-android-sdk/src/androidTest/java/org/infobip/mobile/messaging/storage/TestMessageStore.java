/*
 * TestMessageStore.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.storage;

import android.content.Context;

import org.infobip.mobile.messaging.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 19/01/2017.
 */

public class TestMessageStore implements MessageStore {

    @Override
    public List<Message> findAll(Context context) {
        return new ArrayList<>();
    }

    @Override
    public long countAll(Context context) {
        return 0;
    }

    @Override
    public void save(Context context, Message... messages) {
    }

    @Override
    public void deleteAll(Context context) {
    }
}
