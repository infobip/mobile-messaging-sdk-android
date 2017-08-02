package org.infobip.mobile.messaging.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationAction;
import org.infobip.mobile.messaging.NotificationCategory;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.dal.bundle.NotificationCategoryBundleMapper;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.MockActivity;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_MESSAGE;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_NOTIFICATION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_ACTION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_CATEGORY;
import static org.infobip.mobile.messaging.MobileMessagingProperty.EXTRA_INTENT_FLAGS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;


public class NotificationActionTapReceiverTest extends MobileMessagingTestCase {

    private Broadcaster broadcastSender;
    private NotificationManager notificationManagerMock;
    private MobileMessagingCore mobileMessagingCore;
    private ArgumentCaptor<NotificationCategory> notificationCategoryArgumentCaptor;
    private ArgumentCaptor<Intent> intentArgumentCaptor;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private NotificationActionTapReceiver notificationActionTapReceiver;
    private NotificationSettings notificationSettings;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = Mockito.mock(Broadcaster.class);
        notificationManagerMock = Mockito.mock(NotificationManager.class);
        mobileMessagingCore = Mockito.mock(MobileMessagingCore.class);
        notificationCategoryArgumentCaptor = ArgumentCaptor.forClass(NotificationCategory.class);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        notificationActionTapReceiver = new NotificationActionTapReceiver(broadcastSender, mobileMessagingCore);

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
        Message givenMessage = createMessage(context, "SomeMessageId", false);
        String givenTappedActionId = "actionId";
        int givenNotificationId = 1234;
        NotificationAction notificationAction1 = givenNotificationAction("actionIdNotTapped").build();
        NotificationAction notificationAction2 = givenNotificationAction(givenTappedActionId).build();
        NotificationCategory givenNotificationCategory = new NotificationCategory("categoryId", notificationAction1, notificationAction2);
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedActionId, givenNotificationId, notificationSettings.getIntentFlags());

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1))
                .notificationActionTapped(messageArgumentCaptor.capture(), notificationCategoryArgumentCaptor.capture(), eq(givenTappedActionId));
        assertJEquals(givenNotificationCategory, notificationCategoryArgumentCaptor.getValue());
        assertJEquals(givenMessage, messageArgumentCaptor.getValue());

        Mockito.verify(mobileMessagingCore, Mockito.times(1)).setMessagesSeen(givenMessage.getMessageId());
        Mockito.verify(contextMock, Mockito.never()).startActivity(any(Intent.class));
    }

    @Test
    public void test_should_send_action_clicked_event_and_open_activity() throws Exception {

        // Given
        Message givenMessage = createMessage(context, "SomeMessageId", false);
        String givenTappedActionId = "actionId";
        int givenNotificationId = 1234;
        NotificationAction notificationAction1 = givenNotificationAction("actionIdNotTapped").build();
        NotificationAction notificationAction2 = givenNotificationAction(givenTappedActionId)
                .withBringingAppToForeground(true)
                .build();
        NotificationCategory givenNotificationCategory = new NotificationCategory("categoryId", notificationAction1, notificationAction2);
        Intent givenIntent = givenIntent(givenMessage, givenNotificationCategory, givenTappedActionId, givenNotificationId, notificationSettings.getIntentFlags());

        // When
        notificationActionTapReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1))
                .notificationActionTapped(messageArgumentCaptor.capture(), notificationCategoryArgumentCaptor.capture(), eq(givenTappedActionId));
        assertJEquals(givenNotificationCategory, notificationCategoryArgumentCaptor.getValue());
        assertJEquals(givenMessage, messageArgumentCaptor.getValue());

        Mockito.verify(mobileMessagingCore, Mockito.times(1)).setMessagesSeen(givenMessage.getMessageId());
        Mockito.verify(contextMock, Mockito.times(1)).startActivity(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        String actualTappedActionId = intent.getStringExtra(BroadcastParameter.EXTRA_TAPPED_ACTION_ID);
        NotificationCategory actualTappedCategory = NotificationCategory.createFrom(intent.getBundleExtra(BroadcastParameter.EXTRA_TAPPED_CATEGORY));
        Message actualTappedMessage = Message.createFrom(intent.getBundleExtra(EXTRA_MESSAGE));

        assertEquals(notificationSettings.getIntentFlags() | Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
        assertEquals(givenTappedActionId, actualTappedActionId);
        assertJEquals(givenNotificationCategory, actualTappedCategory);
        assertJEquals(givenMessage, actualTappedMessage);
    }

    @NonNull
    private NotificationAction.Builder givenNotificationAction(String givenTappedActionId) {
        return new NotificationAction.Builder()
                .withId(givenTappedActionId)
                .withIcon(android.R.drawable.btn_default)
                .withTitleResourceId(android.R.string.ok);
    }

    private Intent givenIntent(Message message, NotificationCategory notificationCategory, String actionId, int notificationId, int flags) {
        return new Intent(context, NotificationActionTapReceiver.class)
                .putExtra(EXTRA_MESSAGE, MessageBundleMapper.messageToBundle(message))
                .putExtra(EXTRA_TAPPED_ACTION_ID, actionId)
                .putExtra(EXTRA_TAPPED_CATEGORY, NotificationCategoryBundleMapper.notificationCategoryToBundle(notificationCategory))
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .putExtra(EXTRA_INTENT_FLAGS.getKey(), flags);
    }
}
