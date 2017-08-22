package org.infobip.mobile.messaging.mobile.messages;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.shaded.google.gson.Gson;
import org.infobip.mobile.messaging.mobile.common.DefaultRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.notification.NotificationHandlerImpl;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class MessagesSynchronizerTest extends MobileMessagingTestCase {

    private static final int MESSAGE_ID_PARAMETER_LIMIT = 100;

    private MobileMessagingStats mobileMessagingStats;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private MessagesSynchronizer messagesSynchronizer;
    private MRetryPolicy retryPolicy;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mobileMessagingStats = Mockito.mock(MobileMessagingStats.class);
        notificationHandler = Mockito.mock(NotificationHandlerImpl.class);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

        retryPolicy = DefaultRetryPolicy.create(context);

        messagesSynchronizer = new MessagesSynchronizer(context, mobileMessagingCore, mobileMessagingStats, Executors.newSingleThreadExecutor(), broadcaster, retryPolicy, notificationHandler);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        Mockito.reset(mobileMessagingStats, notificationHandler);
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
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        // When
        mobileMessaging.setMessagesDelivered("1");
        mobileMessaging.setMessagesDelivered("2");
        mobileMessaging.setMessagesDelivered("3");
        mobileMessaging.setMessagesDelivered("4");
        mobileMessaging.setMessagesDelivered("5");

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).deliveryReported(Mockito.<String>anyVararg());
        List<String> actualDLRs = getReportedDLRs();
        assertEquals(5, actualDLRs.size());
        assertTrue(actualDLRs.containsAll(Arrays.asList("1", "2", "3", "4", "5")));
    }

    @Test
    public void should_deserialize_messages_with_appropriate_vibration_from_fetched_payload() {

        // Given
        mobileMessagingCore.getAndRemoveUnreportedMessageIds();
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{" +
                "\"payloads\":[{" +
                "   \"gcm.notification.messageId\":\"someMessageId1\"," +
                "   \"gcm.notification.body\":\"someBody1\"" +
                "}," +
                "{" +
                "   \"gcm.notification.messageId\":\"someMessageId2\"," +
                "   \"gcm.notification.body\":\"someBody2\"," +
                "   \"gcm.notification.vibrate\":\"true\"" +
                "}," +
                "{" +
                "   \"gcm.notification.messageId\":\"someMessageId3\"," +
                "   \"gcm.notification.body\":\"someBody3\"," +
                "   \"gcm.notification.vibrate\":\"false\"" +
                "}]" +
            "}");

        // When
        messagesSynchronizer.sync();

        // Then
        Mockito.verify(notificationHandler, Mockito.after(1000).times(3)).displayNotification(messageArgumentCaptor.capture());
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

    private List<String> getReportedDLRs() {
        Gson gson = new Gson();
        List<String> bodies = debugServer.getBodiesForUri("/mobile/5/messages");
        List<String> ids = new ArrayList<>();
        for (String body : bodies) {
            if (body == null) {
                continue;
            }

            SyncMessagesBody requestBody = gson.fromJson(body, SyncMessagesBody.class);
            if (requestBody.getDrIDs() != null) {
                ids.addAll(Arrays.asList(requestBody.getDrIDs()));
            }
        }
        return ids;
    }
}
