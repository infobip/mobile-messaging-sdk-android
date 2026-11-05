/*
 * MobileInboxTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    private MobileMessaging.ResultListener<Void> deleteResultListener = mock(MobileMessaging.ResultListener.class);


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

        assertFetchValidationError(inboxResultListener, "External user ID must not be blank");
    }

    @Test
    public void fetchInbox_should_complete_once_for_each_blank_token_without_falling_back_to_app_auth() {
        for (String token : new String[]{null, "", " ", "\t\r\n"}) {
            MobileMessaging.ResultListener<Inbox> listener = mock(MobileMessaging.ResultListener.class);

            mobileInboxImpl.fetchInbox(token, givenExternalUserId, filterOptions(), listener);

            assertFetchValidationError(listener, "JWT token must not be blank");
        }
    }

    @Test
    public void fetchInbox_should_complete_once_for_each_blank_user_id_with_token() {
        for (String userId : new String[]{null, "", " ", "\t\r\n"}) {
            MobileMessaging.ResultListener<Inbox> listener = mock(MobileMessaging.ResultListener.class);

            mobileInboxImpl.fetchInbox(givenToken, userId, filterOptions(), listener);

            assertFetchValidationError(listener, "External user ID must not be blank");
        }
    }

    @Test
    public void fetchInbox_should_complete_once_for_each_blank_user_id_with_application_code() {
        for (String userId : new String[]{null, "", " ", "\t\r\n"}) {
            MobileMessaging.ResultListener<Inbox> listener = mock(MobileMessaging.ResultListener.class);

            mobileInboxImpl.fetchInbox(userId, filterOptions(), listener);

            assertFetchValidationError(listener, "External user ID must not be blank");
        }
    }

    @Test
    public void fetchInbox_should_complete_only_once_when_multiple_parameters_are_invalid() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, Collections.singletonList("Offers"), 15);
        options.setCursor("cursor");

        mobileInboxImpl.fetchInbox(" ", " ", options, inboxResultListener);

        assertFetchValidationError(inboxResultListener, "JWT token must not be blank");
    }

    @Test
    public void fetchInbox_should_safely_reject_invalid_credentials_without_listener() {
        for (String blank : new String[]{null, "", " ", "\t\r\n"}) {
            mobileInboxImpl.fetchInbox(blank, givenExternalUserId, null, null);
            mobileInboxImpl.fetchInbox(givenToken, blank, null, null);
            mobileInboxImpl.fetchInbox(blank, null, null);
        }

        Mockito.verify(mobileInboxSynchronizer, times(0)).fetchInbox(any(), any(), any(), any());
    }

    private void assertFetchValidationError(MobileMessaging.ResultListener<Inbox> listener, String message) {
        Mockito.verify(mobileInboxSynchronizer, times(0)).fetchInbox(any(), any(), any(), any());
        ArgumentCaptor<Result> captor = ArgumentCaptor.forClass(Result.class);
        Mockito.verify(listener, times(1)).onResult(captor.capture());
        Mockito.verifyNoMoreInteractions(listener);
        Result result = captor.getValue();
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("20013", result.getError().getCode());
        assertEquals(MobileMessagingError.Type.SDK_ERROR, result.getError().getType());
        assertEquals(message, result.getError().getMessage());
    }

    @Test
    public void fetchInbox_should_be_called_without_filter_options() {
        mobileInboxImpl.fetchInbox(givenExternalUserId, null, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(1)).fetchInbox(null, givenExternalUserId, null, inboxResultListener);
    }

    @Test
    public void deleteMessages_should_be_called_with_provided_token() {
        String[] messageIds = {"id1", "id2"};

        mobileInboxImpl.deleteMessages(givenToken, givenExternalUserId, messageIds, deleteResultListener);

        Mockito.verify(mobileInboxSynchronizer).deleteMessages(
                givenToken, givenExternalUserId, messageIds, deleteResultListener);
    }

    @Test
    public void deleteMessages_should_be_called_with_application_code() {
        String[] messageIds = {"id1", "id2"};

        mobileInboxImpl.deleteMessages(givenExternalUserId, messageIds, deleteResultListener);

        Mockito.verify(mobileInboxSynchronizer).deleteMessages(
                null, givenExternalUserId, messageIds, deleteResultListener);
    }

    @Test
    public void deleteMessages_should_accept_single_id_with_provided_token() {
        String[] messageIds = {"id1"};

        mobileInboxImpl.deleteMessages(givenToken, givenExternalUserId, messageIds, deleteResultListener);

        Mockito.verify(mobileInboxSynchronizer).deleteMessages(
                givenToken, givenExternalUserId, messageIds, deleteResultListener);
    }

    @Test
    public void deleteMessages_should_accept_single_id_with_application_code() {
        String[] messageIds = {"id1"};

        mobileInboxImpl.deleteMessages(givenExternalUserId, messageIds, deleteResultListener);

        Mockito.verify(mobileInboxSynchronizer).deleteMessages(
                null, givenExternalUserId, messageIds, deleteResultListener);
    }

    @Test
    public void deleteMessages_should_reject_blank_message_id() {
        mobileInboxImpl.deleteMessages(givenToken, givenExternalUserId,
                new String[]{"id1", " "}, deleteResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(0)).deleteMessages(any(), any(), any(), any());
        Mockito.verify(deleteResultListener).onResult(any());
    }

    @Test
    public void deleteMessages_should_reject_blank_tokens_with_useful_errors() {
        String[] tokens = {null, "", " \t\n"};
        for (String token : tokens) {
            mobileInboxImpl.deleteMessages(token, givenExternalUserId, new String[]{"id"}, deleteResultListener);
        }
        assertDeleteValidationErrors(tokens.length, "JWT token must not be blank");
    }

    @Test
    public void deleteMessages_should_reject_blank_external_user_ids_for_both_authorization_methods() {
        String[] userIds = {null, "", " \t\n"};
        for (String userId : userIds) {
            mobileInboxImpl.deleteMessages(givenToken, userId, new String[]{"id"}, deleteResultListener);
            mobileInboxImpl.deleteMessages(userId, new String[]{"id"}, deleteResultListener);
        }
        assertDeleteValidationErrors(userIds.length * 2, "External user ID must not be blank");
    }

    private void assertDeleteValidationErrors(int count, String message) {
        Mockito.verify(mobileInboxSynchronizer, times(0)).deleteMessages(any(), any(), any(), any());
        ArgumentCaptor<Result> captor = ArgumentCaptor.forClass(Result.class);
        Mockito.verify(deleteResultListener, times(count)).onResult(captor.capture());
        for (Result result : captor.getAllValues()) {
            assertFalse(result.isSuccess());
            assertEquals(InternalSdkError.INBOX_INVALID_PARAMETERS.getError().getCode(), result.getError().getCode());
            assertEquals(message, result.getError().getMessage());
        }
    }

    @Test
    public void fetchInbox_should_reject_cursor_with_multiple_topics() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, java.util.Arrays.asList("one", "two"), 15);
        options.setCursor("cursor");

        mobileInboxImpl.fetchInbox(givenToken, givenExternalUserId, options, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(0)).fetchInbox(any(), any(), any(), any());
        Mockito.verify(inboxResultListener).onResult(any());
    }

    @Test
    public void fetchInbox_should_reject_cursor_with_single_topic_list_using_token() {
        assertRejectsCursorWithSingleTopicList(givenToken);
    }

    @Test
    public void fetchInbox_should_reject_cursor_with_single_topic_list_using_application_code() {
        assertRejectsCursorWithSingleTopicList(null);
    }

    private void assertRejectsCursorWithSingleTopicList(String token) {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, Collections.singletonList("Offers"), 15);
        options.setCursor("cursor");

        if (token == null) {
            mobileInboxImpl.fetchInbox(givenExternalUserId, options, inboxResultListener);
        } else {
            mobileInboxImpl.fetchInbox(token, givenExternalUserId, options, inboxResultListener);
        }

        Mockito.verify(mobileInboxSynchronizer, times(0)).fetchInbox(any(), any(), any(), any());
        ArgumentCaptor<Result> captor = ArgumentCaptor.forClass(Result.class);
        Mockito.verify(inboxResultListener).onResult(captor.capture());
        assertFalse(captor.getValue().isSuccess());
        assertEquals(InternalSdkError.INBOX_INVALID_PARAMETERS.getError().getCode(),
                captor.getValue().getError().getCode());
    }

    @Test
    public void fetchInbox_should_accept_cursor_with_singular_topic_for_both_authorization_methods() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, "Offers", 15, "cursor");

        mobileInboxImpl.fetchInbox(givenToken, givenExternalUserId, options, inboxResultListener);
        mobileInboxImpl.fetchInbox(givenExternalUserId, options, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer).fetchInbox(givenToken, givenExternalUserId, options, inboxResultListener);
        Mockito.verify(mobileInboxSynchronizer).fetchInbox(null, givenExternalUserId, options, inboxResultListener);
        Mockito.verify(inboxResultListener, times(0)).onResult(any());
    }

    @Test
    public void fetchInbox_should_accept_blank_cursors_with_topics_for_both_authorization_methods() {
        for (String cursor : new String[]{null, "", " \t\n"}) {
            MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                    null, null, Collections.singletonList("Offers"), 15);
            options.setCursor(cursor);

            mobileInboxImpl.fetchInbox(givenToken, givenExternalUserId, options, inboxResultListener);
            mobileInboxImpl.fetchInbox(givenExternalUserId, options, inboxResultListener);

            Mockito.verify(mobileInboxSynchronizer).fetchInbox(givenToken, givenExternalUserId, options, inboxResultListener);
            Mockito.verify(mobileInboxSynchronizer).fetchInbox(null, givenExternalUserId, options, inboxResultListener);
        }
        Mockito.verify(inboxResultListener, times(0)).onResult(any());
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
