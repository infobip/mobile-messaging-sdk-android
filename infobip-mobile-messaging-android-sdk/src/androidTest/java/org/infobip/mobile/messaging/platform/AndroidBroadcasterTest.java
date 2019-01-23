package org.infobip.mobile.messaging.platform;

import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;

/**
 * @author sslavin
 * @since 13/03/2017.
 */

public class AndroidBroadcasterTest extends MobileMessagingTestCase {

    private AndroidBroadcaster broadcastSender;
    private ArgumentCaptor<Intent> intentArgumentCaptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = new AndroidBroadcaster(contextMock);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    }

    @Test
    public void test_should_send_message_broadcast() {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.messageReceived(message);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.MESSAGE_RECEIVED.getKey(), intent.getAction());

        Message messageAfter = Message.createFrom(intent.getExtras());
        assertNotSame(message, messageAfter);
        assertEquals("SomeMessageId", messageAfter.getMessageId());
    }

    @Test
    public void test_should_send_error() throws Exception {
        // Given
        MobileMessagingError error = new MobileMessagingError("SomeCode", "SomeMessage");

        // When
        broadcastSender.error(error);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.API_COMMUNICATION_ERROR.getKey(), intent.getAction());

        MobileMessagingError errorAfter = (MobileMessagingError) intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION);
        assertJEquals(error, errorAfter);
    }

    @Test
    public void test_should_send_push_token_received() {
        // When
        broadcastSender.tokenReceived("SomeCloudToken");

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.TOKEN_RECEIVED.getKey(), intent.getAction());
        assertEquals("SomeCloudToken", intent.getStringExtra(BroadcastParameter.EXTRA_CLOUD_TOKEN));
    }

    @Test
    public void test_should_send_push_registration_created() {
        // When
        broadcastSender.registrationCreated("SomeCloudToken", "SomeDeviceInstanceId");

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.REGISTRATION_CREATED.getKey(), intent.getAction());
        assertEquals("SomeCloudToken", intent.getStringExtra(BroadcastParameter.EXTRA_CLOUD_TOKEN));
        assertEquals("SomeDeviceInstanceId", intent.getStringExtra(BroadcastParameter.EXTRA_INFOBIP_ID));
    }

    @Test
    public void test_should_send_delivery_report_message_ids() {
        // When
        broadcastSender.deliveryReported("id1", "id2");

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.DELIVERY_REPORTS_SENT.getKey(), intent.getAction());
        assertEquals("id1", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[0]);
        assertEquals("id2", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[1]);
    }

    @Test
    public void test_should_send_seen_report_message_ids() {
        // When
        broadcastSender.seenStatusReported("id1", "id2");

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.SEEN_REPORTS_SENT.getKey(), intent.getAction());
        assertEquals("id1", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[0]);
        assertEquals("id2", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[1]);
    }

    @Test
    public void test_should_send_mo_messages() {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.messagesSent(Collections.singletonList(message));

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.MESSAGES_SENT.getKey(), intent.getAction());

        List<Message> messagesAfter = Message.createFrom(intent.<Bundle>getParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES));
        assertEquals(1, messagesAfter.size());
        assertEquals("SomeMessageId", messagesAfter.get(0).getMessageId());

    }

    @Test
    public void test_should_send_user_updated_event() {
        // Given
        User user = new User();
        user.setFirstName("FirstName");
        user.setLastName("LastName");

        // When
        broadcastSender.userUpdated(user);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.USER_UPDATED.getKey(), intent.getAction());

        User userAfter = User.createFrom(intent.getExtras());
        assertNotSame(user, userAfter);
        assertEquals("FirstName", userAfter.getFirstName());
        assertEquals("LastName", userAfter.getLastName());
    }

    @Test
    public void test_should_send_installation_updated_event() throws Exception {

        // Given
        Installation installation = new Installation();
        installation.setPrimaryDevice(true);
        installation.setApplicationUserId("appUserID");
        installation.setLanguage("hr");
        installation.setPushRegistrationEnabled(true);

        // When
        broadcastSender.installationUpdated(installation);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.INSTALLATION_UPDATED.getKey(), intent.getAction());

        Installation installationAfter = Installation.createFrom(intent.getExtras());
        assertJEquals(installation, installationAfter);
    }

    @Test
    public void test_should_send_depersonalize_event() throws Exception {

        // When
        broadcastSender.depersonalized();

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.DEPERSONALIZED.getKey(), intent.getAction());
    }

    @Test
    public void test_should_send_personalize_event() throws Exception {

        // Given
        User user = new User();
        user.setFirstName("FirstName");
        user.setLastName("LastName");

        // When
        broadcastSender.personalized(user);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.PERSONALIZED.getKey(), intent.getAction());

        User userAfter = User.createFrom(intent.getExtras());
        assertJEquals(user, userAfter);
    }

    @Test
    public void test_should_set_package_for_intent() throws Exception {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);
        Mockito.when(contextMock.getPackageName()).thenReturn("test.package.name");

        // When
        broadcastSender.messageReceived(message);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());
        assertEquals("test.package.name", intentArgumentCaptor.getValue().getPackage());
    }

    @Test
    public void test_should_send_notification_tapped_event() throws Exception {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.notificationTapped(message);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.NOTIFICATION_TAPPED.getKey(), intent.getAction());

        Message messageAfter = Message.createFrom(intent.getExtras());
        assertNotSame(message, messageAfter);
        assertEquals("SomeMessageId", messageAfter.getMessageId());
    }
}
