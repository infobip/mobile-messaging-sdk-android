package org.infobip.mobile.messaging.cloud;

import android.content.Intent;
import android.os.Bundle;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author sslavin
 * @since 05/09/2018.
 */
public class MobileMessagingCloudServiceTest extends TestCase {

    private RegistrationTokenHandler registrationTokenHandler = Mockito.mock(RegistrationTokenHandler.class);
    private MobileMessageHandler mobileMessageHandler = Mockito.mock(MobileMessageHandler.class);
    private ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

    private MobileMessagingCloudService service = new MobileMessagingCloudService(registrationTokenHandler, mobileMessageHandler);

    @Before
    public void beforeEach() {
        Mockito.reset(registrationTokenHandler, mobileMessageHandler);
    }

    public void test_shouldHandleMessage() {

        Message message = new Message();
        message.setBody("body");
        message.setMessageId("messageId");
        Bundle bundle = MessageBundleMapper.messageToBundle(message);
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.MESSAGE_RECEIVE")
                .putExtras(bundle);

        service.onHandleWork(intent);

        Mockito.verify(mobileMessageHandler, Mockito.times(1)).handleMessage(messageArgumentCaptor.capture());
        assertEquals("messageId", messageArgumentCaptor.getValue().getMessageId());
        assertEquals("body", messageArgumentCaptor.getValue().getBody());
    }

    public void test_shouldHandleNewToken() {
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.NEW_TOKEN")
                .putExtra("org.infobip.mobile.messaging.cloud.TOKEN", "token")
                .putExtra("org.infobip.mobile.messaging.cloud.SENDER_ID", "senderId");

        service.onHandleWork(intent);

        Mockito.verify(registrationTokenHandler, Mockito.times(1)).handleNewToken(Mockito.eq("senderId"), Mockito.eq("token"));
    }

    public void test_shouldHandleTokenCleanup() {
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.TOKEN_CLEANUP")
                .putExtra("org.infobip.mobile.messaging.cloud.SENDER_ID", "senderId");

        service.onHandleWork(intent);

        Mockito.verify(registrationTokenHandler, Mockito.times(1)).cleanupToken(Mockito.eq("senderId"));
    }

    public void test_shouldHandleTokenReset() {
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.TOKEN_RESET")
                .putExtra("org.infobip.mobile.messaging.cloud.SENDER_ID", "senderId");

        service.onHandleWork(intent);

        Mockito.verify(registrationTokenHandler, Mockito.times(1)).cleanupToken(Mockito.eq("senderId"));
        Mockito.verify(registrationTokenHandler, Mockito.times(1)).acquireNewToken(Mockito.eq("senderId"));
    }

    public void test_shouldHandleTokenAcquisition() {
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.TOKEN_ACQUIRE")
                .putExtra("org.infobip.mobile.messaging.cloud.SENDER_ID", "senderId");

        service.onHandleWork(intent);

        Mockito.verify(registrationTokenHandler, Mockito.times(1)).acquireNewToken(Mockito.eq("senderId"));
    }
}
