/*
 * MobileInboxSubclassCompatibilityTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MobileInboxSubclassCompatibilityTest {
    private final MobileInbox inbox = new LegacyInbox();
    private final MobileMessaging.ResultListener<Void> listener = new MobileMessaging.ResultListener<Void>() {
        @Override
        public void onResult(Result<Void, MobileMessagingError> result) {
            fail("Unsupported deletion must throw synchronously without invoking the listener");
        }
    };

    @Test
    public void legacySubclassRejectsBatchDeletionWithToken() {
        assertUnsupported(() -> inbox.deleteMessages("token", "user", new String[]{"id"}, listener));
    }

    @Test
    public void legacySubclassRejectsBatchDeletionWithApplicationCode() {
        assertUnsupported(() -> inbox.deleteMessages("user", new String[]{"id"}, listener));
    }

    @Test
    public void legacySubclassRejectsSingleDeletionWithToken() {
        assertUnsupported(() -> inbox.deleteMessage("token", "user", "id", listener));
    }

    @Test
    public void legacySubclassRejectsSingleDeletionWithApplicationCode() {
        assertUnsupported(() -> inbox.deleteMessage("user", "id", listener));
    }

    private void assertUnsupported(Runnable deletion) {
        try {
            deletion.run();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException error) {
            assertEquals("Inbox deletion is not supported by this implementation", error.getMessage());
        }
    }

    // Intentionally implements only the pre-deletion API: this concrete subclass must still compile.
    private static final class LegacyInbox extends MobileInbox {
        @Override
        public void fetchInbox(String token, String externalUserId, MobileInboxFilterOptions options,
                               MobileMessaging.ResultListener<Inbox> listener) {
        }

        @Override
        public void fetchInbox(String externalUserId, MobileInboxFilterOptions options,
                               MobileMessaging.ResultListener<Inbox> listener) {
        }

        @Override
        public void setSeen(String externalUserId, String[] messageIds,
                            MobileMessaging.ResultListener<String[]> listener) {
        }
    }
}
