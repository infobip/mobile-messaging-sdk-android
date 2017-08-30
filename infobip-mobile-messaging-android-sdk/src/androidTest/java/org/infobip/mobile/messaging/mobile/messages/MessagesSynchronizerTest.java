package org.infobip.mobile.messaging.mobile.messages;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.messages.SyncMessagesResponse;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.mobile.common.DefaultRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class MessagesSynchronizerTest extends MobileMessagingTestCase {

    private static final int MESSAGE_ID_PARAMETER_LIMIT = 100;

    private ArgumentCaptor<Message> messageArgumentCaptor;
    private MessagesSynchronizer messagesSynchronizer;
    private MRetryPolicy retryPolicy;
    private MobileMessageHandler mobileMessageHandler;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mobileMessageHandler = mock(MobileMessageHandler.class);
        messageArgumentCaptor = forClass(Message.class);

        retryPolicy = DefaultRetryPolicy.create(context);

        messagesSynchronizer = new MessagesSynchronizer(mobileMessagingCore, mobileMessagingCore.getStats(),
                Executors.newSingleThreadExecutor(), broadcaster, retryPolicy, mobileMessageHandler, mobileApiMessages);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Mockito.reset(mobileMessageHandler);
    }

    @Test
    public void should_find_all_messageIDs() {
        String[] mockIDs = new String[120];
        for (int i = 0; i < mockIDs.length; i++) {
            mockIDs[i] = UUID.randomUUID().toString();
        }

        mobileMessagingCore.addSyncMessagesIds(mockIDs);
        String[] messageIDs = mobileMessagingCore.getSyncMessagesIds();

        assertNotNull(messageIDs);
        assertTrue(MESSAGE_ID_PARAMETER_LIMIT >= messageIDs.length);
    }

    @Test
    public void should_find_all_no_duplicates_and_nulls() {
        String mockId = UUID.randomUUID().toString();
        String[] mockIDs = new String[10];
        for (int i = 0; i < mockIDs.length; i++) {
            mockIDs[i] = i % 2 == 0 ? mockId : null;
        }

        mobileMessagingCore.addSyncMessagesIds(mockIDs);
        String[] messageIDs = mobileMessagingCore.getSyncMessagesIds();

        assertNotNull(messageIDs);
        assertTrue(messageIDs.length == 1);
    }

    @Test
    public void should_not_report_dlr_with_duplicate_messageIds() {
        // Given
        mobileMessagingCore.getAndRemoveUnreportedMessageIds();
        // Workaround due to mockito/captor issues
        final List<String> drIds = new ArrayList<>();
        given(mobileApiMessages.sync(any(SyncMessagesBody.class)))
                .willAnswer(new Answer<SyncMessagesResponse>() {
                    @Override
                    public SyncMessagesResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
                        drIds.addAll(asList(invocationOnMock.getArgumentAt(0, SyncMessagesBody.class).getDrIDs()));
                        return new SyncMessagesResponse();
                    }
                });

        // When
        mobileMessagingCore.setMessagesDelivered("1");
        mobileMessagingCore.setMessagesDelivered("2");
        mobileMessagingCore.setMessagesDelivered("3");
        mobileMessagingCore.setMessagesDelivered("4");
        mobileMessagingCore.setMessagesDelivered("5");

        // Then
        verify(mobileApiMessages, after(1000).atMost(5)).sync(any(SyncMessagesBody.class));
        assertEquals(5, drIds.size());
        assertTrue(drIds.containsAll(asList("1", "2", "3", "4", "5")));
    }

    @Test
    public void should_deserialize_messages_with_appropriate_vibration_from_fetched_payload() {

        // Given
        mobileMessagingCore.getAndRemoveUnreportedMessageIds();
        given(mobileApiMessages.sync(any(SyncMessagesBody.class)))
                .willReturn(new SyncMessagesResponse(asList(
                        new MessageResponse() {{
                            setMessageId("someMessageId1");
                            setBody("someBody1");
                        }},
                        new MessageResponse() {{
                            setMessageId("someMessageId2");
                            setBody("someBody2");
                            setVibrate("true");
                        }},
                        new MessageResponse() {{
                            setMessageId("someMessageId3");
                            setBody("someBody3");
                            setVibrate("false");
                        }}
                )));

        // When
        messagesSynchronizer.sync();

        // Then
        verify(mobileMessageHandler, after(1000).times(3)).handleMessage(messageArgumentCaptor.capture());
        List<Message> actualMessages = messageArgumentCaptor.getAllValues();
        assertEquals("someMessageId1", actualMessages.get(0).getMessageId());
        assertEquals("someBody1", actualMessages.get(0).getBody());
        assertEquals(true, actualMessages.get(0).isVibrate());
        assertEquals("someMessageId2", actualMessages.get(1).getMessageId());
        assertEquals("someBody2", actualMessages.get(1).getBody());
        assertEquals(true, actualMessages.get(1).isVibrate());
        assertEquals("someMessageId3", actualMessages.get(2).getMessageId());
        assertEquals("someBody3", actualMessages.get(2).getBody());
        assertEquals(false, actualMessages.get(2).isVibrate());
    }

    private static List<String> getReportedDLRs(List<SyncMessagesBody> bodies) {
        List<String> ids = new ArrayList<>();
        for (SyncMessagesBody body : bodies) {
            if (body.getDrIDs() != null) {
                ids.addAll(asList(body.getDrIDs()));
            }
        }
        return ids;
    }
}
