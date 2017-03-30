package org.infobip.mobile.messaging.notification;

import android.content.Intent;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.dal.bundle.BundleMapper;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.MockActivity;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NotificationTapReceiverTest extends MobileMessagingTestCase {

    private ArgumentCaptor<Intent> intentArgumentCaptor;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private NotificationSettings notificationSettings;
    private NotificationTapReceiver notificationTapReceiver;
    private Broadcaster broadcastSender;
    private MobileMessagingCore mobileMessagingCore;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = new AndroidBroadcaster(contextMock);
        mobileMessagingCore = Mockito.mock(MobileMessagingCore.class);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        notificationTapReceiver = new NotificationTapReceiver(broadcastSender, mobileMessagingCore);

        notificationSettings = new NotificationSettings.Builder(context)
                .withDefaultIcon(android.R.drawable.ic_dialog_alert) // if not set throws -> IllegalArgumentException("defaultIcon doesn't exist");
                .withCallbackActivity(MockActivity.class)
                .build();
        Mockito.when(mobileMessagingCore.getNotificationSettings()).thenReturn(notificationSettings);

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.MARK_SEEN_ON_NOTIFICATION_TAP, true);
    }

    @Test
    public void test_should_set_flags_and_message_for_activity_intent() throws Exception {

        // Given
        Message givenMessage = createMessage(context, "SomeMessageId", false);
        Intent givenIntent = givenIntent(givenMessage, notificationSettings.getIntentFlags());

        // When
        notificationTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).startActivity(intentArgumentCaptor.capture());
        Intent intent = intentArgumentCaptor.getValue();

        assertEquals(notificationSettings.getIntentFlags() | Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
        Message message = Message.createFrom(intent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE));
        assertJEquals(givenMessage, message);
    }

    @Test
    public void test_should_send_notification_tapped_event() throws Exception {

        // Given
        Message givenMessage = createMessage(context, "SomeMessageId", false);
        Intent givenIntent = givenIntent(givenMessage, notificationSettings.getIntentFlags());

        // When
        notificationTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(broadcastSender, Mockito.times(1)).notificationTapped(messageArgumentCaptor.capture());
        assertJEquals(givenMessage, messageArgumentCaptor.getValue());
    }

    @Test
    public void test_should_send_seen_report_message_ids() {

        // Given
        Message givenMessage = createMessage(context, "SomeMessageId", false);
        Intent givenIntent = givenIntent(givenMessage, notificationSettings.getIntentFlags());

        // When
        notificationTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(contextMock, Mockito.times(2)).sendBroadcast(intentArgumentCaptor.capture());
        Intent intent = intentArgumentCaptor.getValue();

        assertEquals(Event.SEEN_REPORTS_SENT.getKey(), intent.getAction());
        assertEquals("SomeMessageId", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[0]);
    }

    @Test
    public void test_should_not_send_seen_report_message_ids_when_seen_on_tap_disabled() {

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.MARK_SEEN_ON_NOTIFICATION_TAP, false);

        // Given
        Message givenMessage = createMessage(context, "SomeMessageId", false);
        Intent givenIntent = givenIntent(givenMessage, notificationSettings.getIntentFlags());

        // When
        notificationTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());
        Intent intent = intentArgumentCaptor.getValue();

        assertNotEquals(Event.SEEN_REPORTS_SENT.getKey(), intent.getAction());
    }

    private Intent givenIntent(Message message, int flags) {
        return new Intent(context, NotificationTapReceiver.class)
                .putExtra(BroadcastParameter.EXTRA_MESSAGE, BundleMapper.messageToBundle(message))
                .putExtra(MobileMessagingProperty.EXTRA_INTENT_FLAGS.getKey(), flags);
    }

    private ArgumentMatcher<Intent> intentMatcher(final String action) {
        return new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Object argument) {
                return ((Intent) argument).getAction().equals(action);
            }
        };
    }
}
