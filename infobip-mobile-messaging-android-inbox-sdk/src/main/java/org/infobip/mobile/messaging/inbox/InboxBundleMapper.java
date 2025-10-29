/*
 * InboxBundleMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import android.os.Bundle;

import org.infobip.mobile.messaging.dal.bundle.BundleMapper;

import androidx.annotation.NonNull;

public class InboxBundleMapper extends BundleMapper {

    private static final String BUNDLED_INBOX_TAG = InboxBundleMapper.class.getName() + ".inbox";
    private static final String BUNDLED_INBOX_MESSAGE_TAG = InboxBundleMapper.class.getName() + ".inbox.message";

    /**
     * De-serializes inbox from bundle
     *
     * @param bundle where to load data from
     * @return new inbox object
     */
    @NonNull
    public static Inbox inboxFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_INBOX_TAG, Inbox.class);
    }

    /**
     * Serializes inbox message to bundle
     *
     * @param inbox object to serialize
     * @return bundle with inbox contents
     */
    @NonNull
    public static Bundle inboxToBundle(@NonNull Inbox inbox) {
        return objectToBundle(inbox, BUNDLED_INBOX_TAG);
    }
}
