package org.infobip.mobile.messaging.interactive.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.app.CallbackActivityStarterWrapper;
import org.infobip.mobile.messaging.interactive.MobileInteractiveImpl;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.platform.InteractiveBroadcaster;
import org.infobip.mobile.messaging.interactive.platform.MockActivity;
import org.infobip.mobile.messaging.interactive.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;


public class NotificationActionTapReceiverTest extends MobileMessagingTestCase {

    private InteractiveBroadcaster broadcastSender;
    private NotificationManager notificationManagerMock;
    private MobileInteractiveImpl mobileInteractive;
    private MobileMessagingCore mobileMessagingCore;
    private ArgumentCaptor<NotificationAction> notificationActionArgumentCaptor;
    private ArgumentCaptor<NotificationCategory> notificationCategoryArgumentCaptor;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private NotificationActionTapReceiver notificationActionTapReceiver;
    private CallbackActivityStarterWrapper callbackActivityStarterWrapper;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = Mockito.mock(InteractiveBroadcaster.class);
        notificationManagerMock = Mockito.mock(NotificationManager.class);
        mobileMessagingCore = Mockito.mock(MobileMessagingCore.class);
        mobileInteractive = Mockito.mock(MobileInteractiveImpl.class);
        callbackActivityStarterWrapper = Mockito.mock(CallbackActivityStarterWrapper.class);
        notificationCategoryArgumentCaptor = ArgumentCaptor.forClass(NotificationCategory.class);
        notificationActionArgumentCaptor = ArgumentCaptor.forClass(NotificationAction.class);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        notificationActionTapReceiver = new NotificationActionTapReceiver(broadcastSender, mobileMessagingCore, mobileInteractive, callbackActivityStarterWrapper);

        NotificationSettings notificationSettings = new NotificationSettings.Builder(context)
                .withDefaultIcon(android.R.drawable.ic_dialog_alert) // if not set throws -> IllegalArgumentException("defaultIcon doesn't exist");
                .withCallbackActivity(MockActivity.class)
                .build();
        Mockito.when(mobileMessagingCore.getNotificationSettings()).thenReturn(notificationSettings);

        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(eq(Context.NOTIFICATION_SERVICE))).thenReturn(notificationManagerMock);
    }

    @Test
    public void test_should_send_notification_action_clicked_event() throws Exception {

        // Given
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId").build();
        NotificationCategory givenNotificationCategory = givenNotificationCategory(givenTappedNotificationAction);
        Message givenMessage = createMessage(context, "SomeMessageId", givenNotificationCategory.getCategoryId(), false);
        int givenNotificationId = 1234;
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedNotificationAction, givenNotificationId);

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1)).notificationActionTapped(
                messageArgumentCaptor.capture(),
                notificationCategoryArgumentCaptor.capture(),
                notificationActionArgumentCaptor.capture());

        verifyProperPayloadWasSentAndActionsTriggered(givenTappedNotificationAction, givenNotificationCategory, givenMessage);
        Mockito.verify(contextMock, Mockito.never()).startActivity(any(Intent.class));
    }

    @Test
    public void test_should_send_action_clicked_event_and_open_activity_if_action_should_bring_app_to_foreground() throws Exception {

        // Given
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId")
                .withBringingAppToForeground(true)
                .build();
        NotificationCategory givenNotificationCategory = givenNotificationCategory(givenTappedNotificationAction);
        Message givenMessage = createMessage(context, "SomeMessageId", givenNotificationCategory.getCategoryId(), false);
        int givenNotificationId = 1234;
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedNotificationAction, givenNotificationId);
        Mockito.when(broadcastSender.notificationActionTapped(any(Message.class), any(NotificationCategory.class), any(NotificationAction.class))).thenReturn(givenIntent);

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1)).notificationActionTapped(
                messageArgumentCaptor.capture(),
                notificationCategoryArgumentCaptor.capture(),
                notificationActionArgumentCaptor.capture());

        verifyProperPayloadWasSentAndActionsTriggered(givenTappedNotificationAction, givenNotificationCategory, givenMessage);
        Mockito.verify(callbackActivityStarterWrapper, Mockito.times(1)).startActivity(givenIntent, true);
    }

    @Test
    public void test_should_send_action_clicked_event_and_not_open_activity_if_action_should_not_bring_app_to_foreground() throws Exception {

        // Given
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId")
                .withBringingAppToForeground(false)
                .build();
        NotificationCategory givenNotificationCategory = givenNotificationCategory(givenTappedNotificationAction);
        Message givenMessage = createMessage(context, "SomeMessageId", givenNotificationCategory.getCategoryId(), false);
        int givenNotificationId = 1234;
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedNotificationAction, givenNotificationId);
        Mockito.when(broadcastSender.notificationActionTapped(any(Message.class), any(NotificationCategory.class), any(NotificationAction.class))).thenReturn(givenIntent);

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1)).notificationActionTapped(
                messageArgumentCaptor.capture(),
                notificationCategoryArgumentCaptor.capture(),
                notificationActionArgumentCaptor.capture());

        verifyProperPayloadWasSentAndActionsTriggered(givenTappedNotificationAction, givenNotificationCategory, givenMessage);
        Mockito.verify(callbackActivityStarterWrapper, Mockito.never()).startActivity(givenIntent, true);
        Mockito.verify(callbackActivityStarterWrapper, Mockito.never()).startActivity(givenIntent, false);
    }

    @Test
    public void test_should_trigger_sdk_actions_when_clicked_on_action_button() throws Exception {

        // Given
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId")
                .withMoMessage()
                .build();
        NotificationCategory givenNotificationCategory = givenNotificationCategory(givenTappedNotificationAction);
        Message givenMessage = createMessage(context, "SomeMessageId", givenNotificationCategory.getCategoryId(), false);
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedNotificationAction, 1234);

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(broadcastSender, Mockito.times(1)).notificationActionTapped(
                messageArgumentCaptor.capture(),
                notificationCategoryArgumentCaptor.capture(),
                notificationActionArgumentCaptor.capture());
        Message actualMessage = messageArgumentCaptor.getValue();
        NotificationAction actualAction = notificationActionArgumentCaptor.getValue();
        Mockito.verify(mobileInteractive, Mockito.times(1)).triggerSdkActionsFor(actualAction, actualMessage);
    }

    private void verifyProperPayloadWasSentAndActionsTriggered(NotificationAction givenTappedNotificationAction, NotificationCategory givenNotificationCategory, Message givenMessage) throws Exception {
        NotificationAction actualAction = notificationActionArgumentCaptor.getValue();
        NotificationCategory actualCategory = notificationCategoryArgumentCaptor.getValue();
        Message actualMessage = messageArgumentCaptor.getValue();
        assertJEquals(givenTappedNotificationAction, actualAction);
        assertJEquals(givenNotificationCategory, actualCategory);
        assertJEquals(givenMessage, actualMessage);

        Mockito.verify(mobileInteractive, Mockito.times(1)).triggerSdkActionsFor(actualAction, actualMessage);
    }
}
