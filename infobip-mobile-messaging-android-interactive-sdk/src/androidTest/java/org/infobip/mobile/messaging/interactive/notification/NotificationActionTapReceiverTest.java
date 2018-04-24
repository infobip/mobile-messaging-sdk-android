package org.infobip.mobile.messaging.interactive.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.interactive.MobileInteractiveImpl;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.platform.InteractiveBroadcaster;
import org.infobip.mobile.messaging.interactive.platform.MockActivity;
import org.infobip.mobile.messaging.interactive.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;


public class NotificationActionTapReceiverTest extends MobileMessagingTestCase {

    private InteractiveBroadcaster broadcastSender;
    private NotificationManager notificationManagerMock;
    private MobileInteractiveImpl mobileInteractive;
    private MobileMessagingCore mobileMessagingCore;
    private ArgumentCaptor<NotificationAction> notificationActionArgumentCaptor;
    private ArgumentCaptor<NotificationCategory> notificationCategoryArgumentCaptor;
    private ArgumentCaptor<Intent> intentArgumentCaptor;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private NotificationActionTapReceiver notificationActionTapReceiver;
    private NotificationSettings notificationSettings;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = Mockito.mock(InteractiveBroadcaster.class);
        notificationManagerMock = Mockito.mock(NotificationManager.class);
        mobileMessagingCore = Mockito.mock(MobileMessagingCore.class);
        mobileInteractive = Mockito.mock(MobileInteractiveImpl.class);
        notificationCategoryArgumentCaptor = ArgumentCaptor.forClass(NotificationCategory.class);
        notificationActionArgumentCaptor = ArgumentCaptor.forClass(NotificationAction.class);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        notificationActionTapReceiver = new NotificationActionTapReceiver(broadcastSender, mobileMessagingCore, mobileInteractive);

        notificationSettings = new NotificationSettings.Builder(context)
                .withDefaultIcon(android.R.drawable.ic_dialog_alert) // if not set throws -> IllegalArgumentException("defaultIcon doesn't exist");
                .withCallbackActivity(MockActivity.class)
                .build();
        Mockito.when(mobileMessagingCore.getNotificationSettings()).thenReturn(notificationSettings);

        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.NOTIFICATION_SERVICE))).thenReturn(notificationManagerMock);
    }

    @Test
    public void test_should_send_notification_action_clicked_event() throws Exception {

        // Given
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId").build();
        NotificationCategory givenNotificationCategory = givenNotificationCategory(givenTappedNotificationAction);
        Message givenMessage = createMessage(context, "SomeMessageId", givenNotificationCategory.getCategoryId(), false);
        int givenNotificationId = 1234;
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedNotificationAction, givenNotificationId, notificationSettings.getIntentFlags());

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1)).notificationActionTapped(
                messageArgumentCaptor.capture(),
                notificationCategoryArgumentCaptor.capture(),
                notificationActionArgumentCaptor.capture());
        NotificationAction actualAction = notificationActionArgumentCaptor.getValue();
        NotificationCategory actualCategory = notificationCategoryArgumentCaptor.getValue();
        Message actualMessage = messageArgumentCaptor.getValue();

        assertJEquals(givenTappedNotificationAction, actualAction);
        assertJEquals(givenNotificationCategory, actualCategory);
        assertJEquals(givenMessage, actualMessage);

        Mockito.verify(mobileInteractive, Mockito.times(1)).triggerSdkActionsFor(actualAction, actualMessage);
        Mockito.verify(contextMock, Mockito.never()).startActivity(any(Intent.class));
    }

    @Test
    public void test_should_send_action_clicked_event_and_open_activity() throws Exception {

        // Given
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId")
                .withBringingAppToForeground(true)
                .build();
        NotificationCategory givenNotificationCategory = givenNotificationCategory(givenTappedNotificationAction);
        Message givenMessage = createMessage(context, "SomeMessageId", givenNotificationCategory.getCategoryId(), false);
        int givenNotificationId = 1234;
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedNotificationAction, givenNotificationId, notificationSettings.getIntentFlags());

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1)).notificationActionTapped(
                messageArgumentCaptor.capture(),
                notificationCategoryArgumentCaptor.capture(),
                notificationActionArgumentCaptor.capture());
        Message actualMessage = messageArgumentCaptor.getValue();
        NotificationAction actualAction = notificationActionArgumentCaptor.getValue();
        NotificationCategory actualCategory = notificationCategoryArgumentCaptor.getValue();

        assertJEquals(givenNotificationCategory, actualCategory);
        assertJEquals(givenMessage, actualMessage);
        assertJEquals(givenTappedNotificationAction, actualAction);

        Mockito.verify(mobileInteractive, Mockito.times(1)).triggerSdkActionsFor(actualAction, actualMessage);
        Mockito.verify(contextMock, Mockito.times(1)).startActivity(intentArgumentCaptor.capture());

        Intent actualIntent = intentArgumentCaptor.getValue();
        NotificationAction actualTappedAction = NotificationAction.createFrom(actualIntent.getExtras());
        NotificationCategory actualTappedCategory = NotificationCategory.createFrom(actualIntent.getExtras());
        Message actualTappedMessage = Message.createFrom(actualIntent.getExtras());

        assertEquals(givenIntent.getAction(), actualIntent.getAction());
        assertEquals(notificationSettings.getIntentFlags() | Intent.FLAG_ACTIVITY_NEW_TASK, actualIntent.getFlags());
        assertJEquals(givenTappedNotificationAction, actualTappedAction);
        assertJEquals(givenNotificationCategory, actualTappedCategory);
        assertJEquals(givenMessage, actualTappedMessage);
    }

    @Test
    public void test_should_trigger_sdk_actions_when_clicked_on_action_button() throws Exception {

        // Given
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId")
                .withMoMessage()
                .build();
        NotificationCategory givenNotificationCategory = givenNotificationCategory(givenTappedNotificationAction);
        Message givenMessage = createMessage(context, "SomeMessageId", givenNotificationCategory.getCategoryId(), false);
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedNotificationAction, 1234, notificationSettings.getIntentFlags());

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(broadcastSender, Mockito.times(1)).notificationActionTapped(
                messageArgumentCaptor.capture(),
                notificationCategoryArgumentCaptor.capture(),
                notificationActionArgumentCaptor.capture());
        Message actualMessage = messageArgumentCaptor.getValue();
        NotificationAction actualAction = notificationActionArgumentCaptor.getValue();
        NotificationCategory actualCategory = notificationCategoryArgumentCaptor.getValue();
        Mockito.verify(mobileInteractive, Mockito.times(1)).triggerSdkActionsFor(actualAction, actualMessage);
    }
}
