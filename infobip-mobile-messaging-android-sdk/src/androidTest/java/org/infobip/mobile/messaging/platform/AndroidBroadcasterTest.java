package org.infobip.mobile.messaging.platform;

import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
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
        Message expectedMessage = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.messageReceived(expectedMessage);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.MESSAGE_RECEIVED.getKey(), intent.getAction());

        Message message = Message.createFrom(intent.getExtras());
        assertNotSame(expectedMessage, message);
        assertEquals(expectedMessage.getMessageId(), message.getMessageId());
    }

    @Test
    public void test_should_send_error() throws Exception {
        // Given
        MobileMessagingError expectedError = new MobileMessagingError("SomeCode", "SomeMessage");

        // When
        broadcastSender.error(expectedError);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.API_COMMUNICATION_ERROR.getKey(), intent.getAction());

        MobileMessagingError error = (MobileMessagingError) intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION);
        assertJEquals(expectedError, error);
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
        Message expectedMessage = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.messagesSent(Collections.singletonList(expectedMessage));

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.MESSAGES_SENT.getKey(), intent.getAction());

        List<Message> messages = Message.createFrom(intent.<Bundle>getParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES));
        assertEquals(1, messages.size());
        assertEquals(expectedMessage.getMessageId(), messages.get(0).getMessageId());

    }

    @Test
    public void test_should_send_user_updated_event() {
        // Given
        User expectedUser = new User();
        expectedUser.setFirstName("FirstName");
        expectedUser.setLastName("LastName");

        // When
        broadcastSender.userUpdated(expectedUser);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.USER_UPDATED.getKey(), intent.getAction());

        User user = User.createFrom(intent.getExtras());
        assertNotSame(expectedUser, user);
        assertEquals(expectedUser.getFirstName(), user.getFirstName());
        assertEquals(expectedUser.getLastName(), user.getLastName());
    }

    @Test
    public void test_should_send_installation_updated_event() throws Exception {

        // Given
        Installation expectedInstallation = new Installation();
        expectedInstallation.setPrimaryDevice(true);
        expectedInstallation.setApplicationUserId("appUserID");
        expectedInstallation.setLanguage("hr");
        expectedInstallation.setPushRegistrationEnabled(true);

        // When
        broadcastSender.installationUpdated(expectedInstallation);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.INSTALLATION_UPDATED.getKey(), intent.getAction());

        Installation installation = Installation.createFrom(intent.getExtras());
        assertJEquals(expectedInstallation, installation);
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
        User expectedUser = new User();
        expectedUser.setFirstName("FirstName");
        expectedUser.setLastName("LastName");

        // When
        broadcastSender.personalized(expectedUser);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.PERSONALIZED.getKey(), intent.getAction());

        User user = User.createFrom(intent.getExtras());
        assertJEquals(expectedUser, user);
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
    public void test_should_send_notification_displayed_event() throws Exception {
        // Given
        Message expectedMessage = createMessage(context, "SomeMessageId", false);
        int expectedNotificationId = 12345;

        // When
        broadcastSender.notificationDisplayed(expectedMessage, expectedNotificationId);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.NOTIFICATION_DISPLAYED.getKey(), intent.getAction());

        Message message = Message.createFrom(intent.getExtras());
        int notificationId = intent.getIntExtra(BroadcastParameter.EXTRA_NOTIFICATION_ID, -1);
        assertNotSame(expectedMessage, message);
        assertEquals(expectedMessage.getMessageId(), message.getMessageId());
        assertEquals(expectedNotificationId, notificationId);
    }

    @Test
    public void test_should_send_notification_tapped_event() throws Exception {
        // Given
        Message expectedMessage = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.notificationTapped(expectedMessage);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.NOTIFICATION_TAPPED.getKey(), intent.getAction());

        Message message = Message.createFrom(intent.getExtras());
        assertNotSame(expectedMessage, message);
        assertEquals(expectedMessage.getMessageId(), message.getMessageId());
    }

    @Test
    public void test_should_send_user_sessions_reported_event() throws Exception {
        // When
        broadcastSender.userSessionsReported();

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.USER_SESSIONS_SENT.getKey(), intent.getAction());
    }

    @Test
    public void test_should_send_user_custom_events_reported_event() throws Exception {
        // When
        broadcastSender.customEventsReported();

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.CUSTOM_EVENTS_SENT.getKey(), intent.getAction());
    }
}
