package org.infobip.mobile.messaging.cloud;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 15/02/2017.
 */

public class MobileMessageHandlerTest extends MobileMessagingTestCase {

    private MobileMessageHandler handler;
    private MessageStore commonStore;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        enableMessageStoreForReceivedMessages();

        handler = new MobileMessageHandler(mobileMessagingCore, broadcaster, notificationHandler, mobileMessagingCore.getMessageStoreWrapper());
        commonStore = MobileMessaging.getInstance(context).getMessageStore();
    }

    @Test
    public void test_shouldSaveNonGeoMessageToUserStore() throws Exception {

        // Given
        Message m = createMessage(context, "SomeMessageId", false);

        // When
        handler.handleMessage(m);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
    }

    @Test
    public void test_shouldSend_messageReceived_forNonGeoMessage() throws Exception {

        // Given
        Message m = createMessage(context, "SomeMessageId", false);

        // When
        handler.handleMessage(m);

        // Then
        verify(broadcaster, atLeastOnce()).messageReceived(Mockito.any(Message.class));
    }

    @Test
    public void test_shouldNotBroadcastDuplicatedMessages() throws Exception {
        // Given
        Message m1 = createMessage(context, "messageId1", false);
        Message m2 = createMessage(context, "messageId2", false);
        Message m3 = createMessage(context, "messageId1", false);

        // When
        handler.handleMessage(m1);
        handler.handleMessage(m2);
        handler.handleMessage(m3);

        // Then
        verify(broadcaster, times(1)).messageReceived(messageWith("messageId1"));
        verify(broadcaster, times(1)).messageReceived(messageWith("messageId2"));
    }

    @Test
    public void test_shouldNotDisplayDuplicatedMessages() throws Exception {
        // Given
        Message m1 = createMessage(context, "messageId1", false);
        Message m2 = createMessage(context, "messageId2", false);
        Message m3 = createMessage(context, "messageId1", false);

        // When
        handler.handleMessage(m1);
        handler.handleMessage(m2);
        handler.handleMessage(m3);

        // Then
        verify(notificationHandler, times(1)).displayNotification(messageWith("messageId1"));
        verify(notificationHandler, times(1)).displayNotification(messageWith("messageId2"));
    }

    private Message messageWith(final String messageId) {
        return argThat(new ArgumentMatcher<Message>() {
            @Override
            public Class<?> type() {
                return ArgumentMatcher.super.type();
            }

            @Override
            public boolean matches(Message o) {
                return messageId.equals((o).getMessageId());
            }
        });
    }
}
