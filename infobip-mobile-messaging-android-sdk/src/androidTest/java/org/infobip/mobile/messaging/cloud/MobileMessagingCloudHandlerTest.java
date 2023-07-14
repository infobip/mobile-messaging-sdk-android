package org.infobip.mobile.messaging.cloud;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
public class MobileMessagingCloudHandlerTest extends TestCase {

    private RegistrationTokenHandler registrationTokenHandler = Mockito.mock(RegistrationTokenHandler.class);
    private MobileMessageHandler mobileMessageHandler = Mockito.mock(MobileMessageHandler.class);
    private ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    private Context context = null;

    private MobileMessagingCloudHandler handler = new MobileMessagingCloudHandler(registrationTokenHandler, mobileMessageHandler);

    @Before
    public void beforeEach() {
        Mockito.reset(registrationTokenHandler, mobileMessageHandler);
    }

    @Test
    public void test_shouldHandleMessage() {

        Message message = new Message();
        message.setBody("body");
        message.setMessageId("messageId");
        Bundle bundle = MessageBundleMapper.messageToBundle(message);
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.MESSAGE_RECEIVE")
                .putExtras(bundle);

        handler.handleWork(context, intent);

        Mockito.verify(mobileMessageHandler, Mockito.times(1)).handleMessage(messageArgumentCaptor.capture());
        assertEquals("messageId", messageArgumentCaptor.getValue().getMessageId());
        assertEquals("body", messageArgumentCaptor.getValue().getBody());
    }

    @Test
    public void test_shouldHandleNewToken() {
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.NEW_TOKEN")
                .putExtra("org.infobip.mobile.messaging.cloud.TOKEN", "token");

        handler.handleWork(context, intent);

        Mockito.verify(registrationTokenHandler, Mockito.times(1)).handleNewToken(Mockito.eq("token"));
    }

    @Test
    public void test_shouldHandleTokenCleanup() {
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.TOKEN_CLEANUP");

        handler.handleWork(context, intent);

        Mockito.verify(registrationTokenHandler, Mockito.times(1)).cleanupToken();
    }

    @Test
    public void test_shouldHandleTokenReset() {
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.TOKEN_RESET");

        handler.handleWork(context, intent);

        Mockito.verify(registrationTokenHandler, Mockito.times(1)).reissueToken();
    }

    @Test
    public void test_shouldHandleTokenAcquisition() {
        Intent intent = new Intent("org.infobip.mobile.messaging.cloud.TOKEN_ACQUIRE");

        handler.handleWork(context, intent);

        Mockito.verify(registrationTokenHandler, Mockito.times(1)).acquireNewToken();
    }
}
