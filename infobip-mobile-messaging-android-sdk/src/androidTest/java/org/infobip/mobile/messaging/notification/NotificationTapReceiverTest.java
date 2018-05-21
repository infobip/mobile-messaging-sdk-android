package org.infobip.mobile.messaging.notification;

import android.content.Intent;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.app.CallbackActivityStarterWrapper;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.MockActivity;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.eq;

public class NotificationTapReceiverTest extends MobileMessagingTestCase {

    private ArgumentCaptor<Intent> intentArgumentCaptor;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private NotificationSettings notificationSettings;
    private NotificationTapReceiver notificationTapReceiver;
    private Broadcaster broadcastSender;
    private MobileMessagingCore mobileMessagingCore;
    private CallbackActivityStarterWrapper callbackActivityStarterWrapper;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = Mockito.mock(Broadcaster.class);
        mobileMessagingCore = Mockito.mock(MobileMessagingCore.class);
        callbackActivityStarterWrapper = Mockito.mock(CallbackActivityStarterWrapper.class);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        notificationTapReceiver = new NotificationTapReceiver(broadcastSender, mobileMessagingCore, callbackActivityStarterWrapper);

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
        Intent expectedIntent = new Intent(Event.NOTIFICATION_TAPPED.getKey());
        expectedIntent.putExtras(givenIntent);
        Mockito.verify(callbackActivityStarterWrapper, Mockito.times(1)).startActivity(intentArgumentCaptor.capture(), eq(true));
        Intent intent = intentArgumentCaptor.getValue();

        assertEquals(notificationSettings.getIntentFlags(), intent.getFlags());
        Message message = Message.createFrom(intent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE));
        assertJEquals(givenMessage, message);
        assertEquals(expectedIntent.getAction(), intent.getAction());
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
        Mockito.verify(mobileMessagingCore, Mockito.times(1)).setMessagesSeen(givenMessage.getMessageId());
    }

    @Test
    public void test_should_not_send_seen_report_message_ids_when_seen_on_tap_disabled() {

        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.MARK_SEEN_ON_NOTIFICATION_TAP, false);
        Message givenMessage = createMessage(context, "SomeMessageId", false);
        Intent givenIntent = givenIntent(givenMessage, notificationSettings.getIntentFlags());

        // When
        notificationTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(mobileMessagingCore, Mockito.never()).setMessagesSeen(Mockito.any(String[].class));
    }

    private Intent givenIntent(Message message, int flags) {
        return new Intent(context, NotificationTapReceiver.class)
                .putExtra(BroadcastParameter.EXTRA_MESSAGE, MessageBundleMapper.messageToBundle(message))
                .addFlags(flags);
    }
}
