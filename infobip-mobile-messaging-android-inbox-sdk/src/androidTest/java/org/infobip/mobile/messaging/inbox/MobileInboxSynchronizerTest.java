/*
 * MobileInboxSynchronizerTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.inbox.MobileApiInbox;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MobileInboxSynchronizerTest extends MobileMessagingTestCase {

    private MobileInboxSynchronizer mobileInboxSynchronizer;
    private AndroidBroadcaster androidBroadcaster;
    private MobileApiInbox mobileApiInbox;
    private MobileMessaging.ResultListener<Inbox> inboxResultListener = mock(MobileMessaging.ResultListener.class);
    private ArgumentCaptor<Inbox> dataCaptor;

    private String givenToken = "someToken";
    private String givenExternalUserId = "someExtUID";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        androidBroadcaster = mock(AndroidBroadcaster.class);
        mobileApiInbox = mock(MobileApiInbox.class);
        dataCaptor = forClass(Inbox.class);

        mobileInboxSynchronizer = new MobileInboxSynchronizer(
                context,
                mobileMessagingCore,
                androidBroadcaster,
                inboxBroadcaster,
                mobileApiInbox
        );

        given(mobileApiInbox.fetchInbox(any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(1, 1, null, null, Collections.singletonList(createMessageResponse("topic1"))));
    }

    @Test
    public void should_call_api_with_bearer() {
        mobileInboxSynchronizer.fetchInbox(givenToken, givenExternalUserId, null, inboxResultListener);
        String resultToken = "Bearer " + givenToken;

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, null, null);
    }

    @Test
    public void should_call_api_with_appcode() {
        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, null, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, null, null);
    }

    @Test
    public void should_call_api_with_only_required_filterOptions() {
        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, "sometopic", 15);
    }

    @Test
    public void should_call_api_with_filterOptions() {
        Date dateFrom = new Date(1640984400000L);
        Date dateTo = new Date(1654462800000L);

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(dateFrom, dateTo, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, "1640984400000", "1654462800000", "sometopic", 15);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();
        assertEquals(1, returnedInbox.getMessages().size());
        assertEquals(1, returnedInbox.getCountUnread());
        assertEquals(1, returnedInbox.getCountTotal());
    }

    @Test
    public void should_call_api_and_fetch_whole_inbox_when_topics_are_defined() {
        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, new ArrayList<>(Arrays.asList("sometopic", "othertopic")), 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, null, 1000);
    }

    @Test
    public void should_call_api_and_work_with_null_messages_response() {
        given(mobileApiInbox.fetchInbox(any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(0, 0, null, null, null));

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();
        assertNull(returnedInbox.getMessages());
    }

    @Test
    public void should_filter_messages_by_multiple_topics() {
        given(mobileApiInbox.fetchInbox(any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(3, 1, null, null, Arrays.asList(
                    createMessageResponse("msg1", "topic1", true),
                    createMessageResponse("msg2", "topic2", true),
                    createMessageResponse("msg3", "topic3", false)
                )));

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, Arrays.asList("topic1", "topic3"), 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();

        assertEquals(2, returnedInbox.getMessages().size());
        assertEquals(2, returnedInbox.getCountTotalFiltered().intValue());
        assertEquals(3, returnedInbox.getCountTotal());
        assertEquals(1, returnedInbox.getCountUnread());
        assertEquals(1, returnedInbox.getCountUnreadFiltered().intValue());
        assertEquals("topic1", returnedInbox.getMessages().get(0).getTopic());
        assertEquals("topic3", returnedInbox.getMessages().get(1).getTopic());
    }

    @Test
    public void should_return_empty_list_when_no_topics_match() {
        given(mobileApiInbox.fetchInbox(any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(2, 2, null, null, Arrays.asList(
                    createMessageResponse("msg1", "topic1", false),
                    createMessageResponse("msg2", "topic2", false)
                )));

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, Arrays.asList("topic5", "topic6"), 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();

        assertEquals(0, returnedInbox.getMessages().size());
    }

    @Test
    public void should_apply_limit_after_topic_filtering() {
        given(mobileApiInbox.fetchInbox(any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(5, 5, null, null, Arrays.asList(
                    createMessageResponse("msg1", "topic1", false),
                    createMessageResponse("msg2", "topic1", false),
                    createMessageResponse("msg3", "topic2", false),
                    createMessageResponse("msg4", "topic1", false),
                    createMessageResponse("msg5", "topic3", false)
                )));

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, Arrays.asList("topic1", "topic2"), 2);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, "App " + mobileMessagingCore.getApplicationCode(), null, null, null, 1000);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();

        assertEquals(2, returnedInbox.getMessages().size());
        assertEquals("topic1", returnedInbox.getMessages().get(0).getTopic());
        assertEquals("topic1", returnedInbox.getMessages().get(1).getTopic());
    }

    @Test
    public void should_not_apply_limit_when_filtered_messages_are_fewer_than_limit() {
        given(mobileApiInbox.fetchInbox(any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(3, 3, null, null, Arrays.asList(
                    createMessageResponse("msg1", "topic1", false),
                    createMessageResponse("msg2", "topic2", false),
                    createMessageResponse("msg3", "topic3", false)
                )));

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, Arrays.asList("topic1", "topic3"), 10);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();

        assertEquals(2, returnedInbox.getMessages().size());
        assertEquals("topic1", returnedInbox.getMessages().get(0).getTopic());
        assertEquals("topic3", returnedInbox.getMessages().get(1).getTopic());
    }
}
