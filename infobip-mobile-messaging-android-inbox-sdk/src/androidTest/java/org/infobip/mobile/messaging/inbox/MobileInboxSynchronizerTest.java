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
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.json.JSONObject;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
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

        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(1, 1, null, null, Collections.singletonList(createMessageResponse("topic1"))));
    }

    @Test
    public void should_call_api_with_bearer() {
        mobileInboxSynchronizer.fetchInbox(givenToken, givenExternalUserId, null, inboxResultListener);
        String resultToken = "Bearer " + givenToken;

        verify(mobileApiInbox, after(300).times(1)).fetchInboxPage(givenExternalUserId, resultToken, null, null, null, null, null);
    }

    @Test
    public void should_call_api_with_appcode() {
        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, null, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInboxPage(givenExternalUserId, resultToken, null, null, null, null, null);
    }

    @Test
    public void should_call_api_with_only_required_filterOptions() {
        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInboxPage(givenExternalUserId, resultToken, null, null, "sometopic", 15, null);
    }

    @Test
    public void should_pass_cursor_and_map_next_cursor() {
        String nextCursor = "next-cursor";
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(1, 1, null, null,
                        Collections.singletonList(createMessageResponse("topic1")), nextCursor));
        MobileInboxFilterOptions filterOptions =
                new MobileInboxFilterOptions(null, null, "sometopic", 15, "current-cursor");

        mobileInboxSynchronizer.fetchInbox(givenToken, givenExternalUserId, filterOptions, inboxResultListener);

        verify(mobileApiInbox, after(300).times(1)).fetchInboxPage(
                givenExternalUserId, "Bearer " + givenToken, null, null, "sometopic", 15, "current-cursor");
        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        assertEquals(nextCursor, dataCaptor.getValue().getNextCursor());
    }

    @Test
    public void should_paginate_without_topic_filter_and_preserve_opaque_cursor() {
        String cursor = "  opaque+/=& cursor  ";
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(null, null, (String) null, 15, cursor);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, options, inboxResultListener);

        verify(mobileApiInbox, after(300)).fetchInboxPage(givenExternalUserId,
                "App " + mobileMessagingCore.getApplicationCode(), null, null, null, 15, cursor);
    }

    @Test
    public void should_paginate_empty_topics_array_without_overriding_limit() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(null, null, Collections.emptyList(), 15);
        options.setCursor("cursor");

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, options, inboxResultListener);

        verify(mobileApiInbox, after(300)).fetchInboxPage(givenExternalUserId,
                "App " + mobileMessagingCore.getApplicationCode(), null, null, null, 15, "cursor");
    }

    @Test
    public void should_normalize_blank_json_cursors_before_sending_request() throws Exception {
        String[] filters = {
                "{\"topic\":\"Offers\",\"limit\":15}",
                "{\"topic\":\"Offers\",\"limit\":15,\"cursor\":null}",
                "{\"topic\":\"Offers\",\"limit\":15,\"cursor\":\"\"}",
                "{\"topic\":\"Offers\",\"limit\":15,\"cursor\":\" \\t\\n\"}"
        };
        for (int i = 0; i < filters.length; i++) {
            MobileInboxFilterOptions options = MobileInboxFilterOptionsJson.mobileInboxFilterOptionsFromJSON(
                    new JSONObject(filters[i]));
            mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, options, inboxResultListener);

            verify(mobileApiInbox, after(300).times(i + 1)).fetchInboxPage(givenExternalUserId,
                    "App " + mobileMessagingCore.getApplicationCode(), null, null, "Offers", 15, null);
        }
    }

    @Test
    public void should_treat_blank_cursor_with_topics_as_legacy_first_page() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, Collections.singletonList("Offers"), 0);
        options.setCursor(" \t\n");
        assertLegacySingleTopicFiltering(options, 50);
    }

    @Test
    public void should_delete_messages_with_bearer_token() {
        String[] messageIds = {"id1", "id2"};

        mobileInboxSynchronizer.deleteMessages(givenToken, givenExternalUserId, messageIds, null);

        verify(mobileApiInbox, after(300).times(1)).deleteInboxMessages(
                givenExternalUserId, "Bearer " + givenToken, messageIds);
    }

    @Test
    public void should_delete_messages_with_application_code() {
        String[] messageIds = {"id1", "id2"};

        mobileInboxSynchronizer.deleteMessages(null, givenExternalUserId, messageIds, null);

        verify(mobileApiInbox, after(300).times(1)).deleteInboxMessages(
                givenExternalUserId, "App " + mobileMessagingCore.getApplicationCode(), messageIds);
    }

    @Test
    public void should_preserve_delete_backend_error_code_and_message() {
        String[] ids = {"id"};
        MobileMessaging.ResultListener<Void> listener = mock(MobileMessaging.ResultListener.class);
        doThrow(new ApiException("DELETE_DENIED", "Deletion is not allowed for this user", null, 403))
                .when(mobileApiInbox).deleteInboxMessages(givenExternalUserId, "Bearer " + givenToken, ids);

        mobileInboxSynchronizer.deleteMessages(givenToken, givenExternalUserId, ids, listener);

        ArgumentCaptor<Result> captor = forClass(Result.class);
        verify(listener, after(300)).onResult(captor.capture());
        assertFalse(captor.getValue().isSuccess());
        assertEquals("DELETE_DENIED", captor.getValue().getError().getCode());
        assertEquals("Deletion is not allowed for this user", captor.getValue().getError().getMessage());
        assertEquals(Integer.valueOf(403), captor.getValue().getError().getHttpStatusCode());
    }

    @Test
    public void should_preserve_invalid_cursor_error_and_http_status_in_fetch_callback() {
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new ApiException("INVALID_CURSOR", "Cursor does not match filters", null, 400));

        mobileInboxSynchronizer.fetchInbox(givenToken, givenExternalUserId,
                new MobileInboxFilterOptions(null, null, "Offers", 20, "cursor"), inboxResultListener);

        ArgumentCaptor<Result> captor = forClass(Result.class);
        verify(inboxResultListener, after(300)).onResult(captor.capture());
        assertFalse(captor.getValue().isSuccess());
        assertEquals("INVALID_CURSOR", captor.getValue().getError().getCode());
        assertEquals("Cursor does not match filters", captor.getValue().getError().getMessage());
        assertEquals(Integer.valueOf(400), captor.getValue().getError().getHttpStatusCode());
    }

    @Test
    public void should_call_api_with_filterOptions() {
        Date dateFrom = new Date(1640984400000L);
        Date dateTo = new Date(1654462800000L);

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(dateFrom, dateTo, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInboxPage(givenExternalUserId, resultToken, "1640984400000", "1654462800000", "sometopic", 15, null);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();
        assertEquals(1, returnedInbox.getMessages().size());
        assertEquals(1, returnedInbox.getCountUnread());
        assertEquals(1, returnedInbox.getCountTotal());
    }

    @Test
    public void should_filter_single_topic_list_without_limit() {
        assertLegacySingleTopicFiltering(new MobileInboxFilterOptions(
                null, null, Collections.singletonList("Offers"), null), 50);
    }

    @Test
    public void should_filter_single_topic_list_with_zero_limit_without_truncating() {
        assertLegacySingleTopicFiltering(new MobileInboxFilterOptions(
                null, null, Collections.singletonList("Offers"), 0), 50);
    }

    @Test
    public void should_apply_positive_limit_after_filtering_single_topic_list() {
        assertLegacySingleTopicFiltering(new MobileInboxFilterOptions(
                null, null, Collections.singletonList("Offers"), 15), 15);
    }

    @Test
    public void should_preserve_single_topic_array_from_json() throws Exception {
        MobileInboxFilterOptions options = MobileInboxFilterOptionsJson.mobileInboxFilterOptionsFromJSON(
                new JSONObject("{\"topics\":[\"Offers\"],\"limit\":0}"));
        assertNull(options.getTopic());
        assertEquals(Collections.singletonList("Offers"), options.getTopics());
        assertLegacySingleTopicFiltering(options, 50);
    }

    private void assertLegacySingleTopicFiltering(MobileInboxFilterOptions options, int expectedSize) {
        List<MessageResponse> messages = new ArrayList<>();
        messages.add(createMessageResponse("other", "Promotions", false));
        for (int i = 0; i < 50; i++) {
            messages.add(createMessageResponse("offer-" + i, "Offers", i % 2 == 0));
        }
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(51, 26, null, null, messages, "server-cursor"));

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, options, inboxResultListener);

        verify(mobileApiInbox, after(300).times(1)).fetchInboxPage(
                givenExternalUserId, "App " + mobileMessagingCore.getApplicationCode(),
                null, null, null, 1000, null);
        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox inbox = dataCaptor.getValue();
        assertEquals(expectedSize, inbox.getMessages().size());
        assertEquals("offer-0", inbox.getMessages().get(0).getMessageId());
        assertEquals("offer-" + (expectedSize - 1), inbox.getMessages().get(expectedSize - 1).getMessageId());
        assertEquals(51, inbox.getCountTotal());
        assertEquals(Integer.valueOf(50), inbox.getCountTotalFiltered());
        assertEquals(Integer.valueOf(25), inbox.getCountUnreadFiltered());
        assertNull(inbox.getNextCursor());
    }

    @Test
    public void should_keep_blank_single_topic_filtering() {
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(2, 2, null, null, Arrays.asList(
                        createMessageResponse("blank", " ", false),
                        createMessageResponse("other", "Promotions", false)), "server-cursor"));
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, Collections.singletonList(" "), 20);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, options, inboxResultListener);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox inbox = dataCaptor.getValue();
        assertEquals(1, inbox.getMessages().size());
        assertEquals("blank", inbox.getMessages().get(0).getMessageId());
        assertNull(inbox.getNextCursor());
    }

    @Test
    public void should_keep_null_single_topic_filtering() {
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(2, 2, null, null, Arrays.asList(
                        createMessageResponse("without-topic", null, false),
                        createMessageResponse("other", "Promotions", false)), "server-cursor"));
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, Collections.singletonList(null), 20);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, options, inboxResultListener);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox inbox = dataCaptor.getValue();
        assertEquals(1, inbox.getMessages().size());
        assertEquals("without-topic", inbox.getMessages().get(0).getMessageId());
        assertNull(inbox.getNextCursor());
    }

    @Test
    public void should_call_api_and_fetch_whole_inbox_when_topics_are_defined() {
        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, new ArrayList<>(Arrays.asList("sometopic", "othertopic")), 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInboxPage(givenExternalUserId, resultToken, null, null, null, 1000, null);
    }

    @Test
    public void should_call_api_and_work_with_null_messages_response() {
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(0, 0, null, null, null));

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();
        assertNull(returnedInbox.getMessages());
    }

    @Test
    public void should_filter_messages_by_multiple_topics() {
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
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
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
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
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(5, 5, null, null, Arrays.asList(
                    createMessageResponse("msg1", "topic1", false),
                    createMessageResponse("msg2", "topic1", false),
                    createMessageResponse("msg3", "topic2", false),
                    createMessageResponse("msg4", "topic1", false),
                    createMessageResponse("msg5", "topic3", false)
                )));

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, Arrays.asList("topic1", "topic2"), 2);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        verify(mobileApiInbox, after(300).times(1)).fetchInboxPage(givenExternalUserId, "App " + mobileMessagingCore.getApplicationCode(), null, null, null, 1000, null);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();

        assertEquals(2, returnedInbox.getMessages().size());
        assertEquals("topic1", returnedInbox.getMessages().get(0).getTopic());
        assertEquals("topic1", returnedInbox.getMessages().get(1).getTopic());
    }

    @Test
    public void should_not_apply_limit_when_filtered_messages_are_fewer_than_limit() {
        given(mobileApiInbox.fetchInboxPage(any(), any(), any(), any(), any(), any(), any()))
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
