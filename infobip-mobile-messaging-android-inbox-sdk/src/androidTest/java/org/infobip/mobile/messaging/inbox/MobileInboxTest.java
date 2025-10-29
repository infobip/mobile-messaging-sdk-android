/*
 * MobileInboxTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class MobileInboxTest extends MobileMessagingTestCase {

    private MobileInboxSynchronizer mobileInboxSynchronizer;
    private InboxSeenStatusReporter inboxSeenStatusReporter;
    private AndroidBroadcaster androidBroadcaster;
    private MobileInboxImpl mobileInboxImpl;
    private MobileMessaging.ResultListener<Inbox> inboxResultListener = mock(MobileMessaging.ResultListener.class);
    private MobileMessaging.ResultListener<String[]> seenResultListener = mock(MobileMessaging.ResultListener.class);


    private String givenToken = "someToken";
    private String givenExternalUserId = "someExtUID";
    private String givenTopic = "someTopic";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        androidBroadcaster = mock(AndroidBroadcaster.class);
        mobileInboxSynchronizer = mock(MobileInboxSynchronizer.class);
        inboxSeenStatusReporter = mock(InboxSeenStatusReporter.class);

        mobileInboxImpl = new MobileInboxImpl(context,
                androidBroadcaster,
                inboxBroadcaster,
                mobileApiResourceProvider,
                mobileInboxSynchronizer,
                inboxSeenStatusReporter
        );
    }

    @Test
    public void fetchInbox_should_be_called_with_null_token() {
        MobileInboxFilterOptions filterOptions = filterOptions();

        mobileInboxImpl.fetchInbox(givenExternalUserId, filterOptions, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(1)).fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);
    }

    @Test
    public void fetchInbox_should_be_called_with_provided_token() {
        MobileInboxFilterOptions filterOptions = filterOptions();

        mobileInboxImpl.fetchInbox(givenToken, givenExternalUserId, filterOptions, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(1)).fetchInbox(givenToken, givenExternalUserId, filterOptions, inboxResultListener);
    }

    @Test
    public void fetchInbox_should_not_be_called_when_externalUserID_is_missing() {
        MobileInboxFilterOptions filterOptions = filterOptions();

        mobileInboxImpl.fetchInbox(null, filterOptions, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(0)).fetchInbox(any(), any(), any(), any());
    }

    @Test
    public void fetchInbox_should_be_called_without_filter_options() {
        mobileInboxImpl.fetchInbox(givenExternalUserId, null, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(1)).fetchInbox(null, givenExternalUserId, null, inboxResultListener);
    }

    @Test
    public void setSeen_should_not_be_called_when_externalUserID_is_missing() {
        mobileInboxImpl.setSeen(null, new String[]{"someMessageId1"}, seenResultListener);

        Mockito.verify(inboxSeenStatusReporter, times(0)).reportSeen(any(), any(), any());
    }

    @Test
    public void setSeen_should_not_be_called_when_messageIDs_are_missing() {
        mobileInboxImpl.setSeen(givenExternalUserId, new String[]{}, seenResultListener);

        Mockito.verify(inboxSeenStatusReporter, times(0)).reportSeen(any(), any(), any());
    }

    @Test
    public void reportSeen_should_report_duplicated_messageIDs_only_once() {
        String[] messageIds = {"messageId1", "messageId1", "messageId2"};

        mobileInboxImpl.setSeen(givenExternalUserId, messageIds, seenResultListener);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(inboxSeenStatusReporter, times(1)).reportSeen(Mockito.eq(seenResultListener), Mockito.eq(givenExternalUserId), captor.capture());

        assertEquals(2, captor.getAllValues().size());

        mobileInboxImpl.setSeen(givenExternalUserId, messageIds, seenResultListener);
        ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
        Mockito.verify(inboxSeenStatusReporter, times(1)).reportSeen(Mockito.eq(seenResultListener), Mockito.eq(givenExternalUserId), captor.capture());
        assertEquals(0, captor1.getAllValues().size());
    }

    @Test
    public void setSeen_should_not_be_called_in_case_of_error() {
        String[] messageIds = {"someMessageId1"};
        Mockito.doThrow(new RuntimeException("Error")).when(inboxSeenStatusReporter).reportSeen(any(), any(), any());

        try {
            mobileInboxImpl.setSeen(givenExternalUserId, messageIds, seenResultListener);
        } catch (RuntimeException e) {
            // Expected exception
        }

        Mockito.verify(seenResultListener, times(0)).onResult(any());
    }

    private MobileInboxFilterOptions filterOptions() {
        return new MobileInboxFilterOptions(
                null, null, givenTopic, 15
        );
    }
}
