package org.infobip.mobile.messaging.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.dal.bundle.NotificationCategoryBundleMapper;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.MockActivity;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_NOTIFICATION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TRIGGERED_ACTION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TRIGGERED_CATEGORY;
import static org.infobip.mobile.messaging.MobileMessagingProperty.EXTRA_INTENT_FLAGS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;


public class NotificationActionReceiverTest extends MobileMessagingTestCase {

    private Broadcaster broadcastSender;
    private NotificationManager notificationManagerMock;
    private ArgumentCaptor<NotificationCategory> notificationCategoryArgumentCaptor;
    private ArgumentCaptor<Intent> intentArgumentCaptor;
    private NotificationActionReceiver notificationActionReceiver;
    private NotificationSettings notificationSettings;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = Mockito.mock(Broadcaster.class);
        notificationManagerMock = Mockito.mock(NotificationManager.class);
        MobileMessagingCore mobileMessagingCore = Mockito.mock(MobileMessagingCore.class);
        notificationCategoryArgumentCaptor = ArgumentCaptor.forClass(NotificationCategory.class);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        notificationActionReceiver = new NotificationActionReceiver(broadcastSender, mobileMessagingCore);

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
        String givenTriggeredActionId = "actionId";
        int givenNotificationId = 1234;
        NotificationAction notificationAction1 = givenNotificationAction("actionIdNotTriggered").build();
        NotificationAction notificationAction2 = givenNotificationAction(givenTriggeredActionId).build();
        NotificationCategory givenNotificationCategory = new NotificationCategory("categoryId", notificationAction1, notificationAction2);
        Intent givenIntent = givenIntent(givenNotificationCategory, givenTriggeredActionId, givenNotificationId, notificationSettings.getIntentFlags());

        // When
        notificationActionReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1))
                .notificationActionTriggered(notificationCategoryArgumentCaptor.capture(), eq(givenTriggeredActionId));
        assertJEquals(givenNotificationCategory, notificationCategoryArgumentCaptor.getValue());

        Mockito.verify(contextMock, Mockito.never()).startActivity(any(Intent.class));
    }

    @Test
    public void test_should_send_action_clicked_event_and_open_activity() throws Exception {

        // Given
        String givenTriggeredActionId = "actionId";
        int givenNotificationId = 1234;
        NotificationAction notificationAction1 = givenNotificationAction("actionIdNotTriggered").build();
        NotificationAction notificationAction2 = givenNotificationAction(givenTriggeredActionId)
                .withBringingAppToForeground(true)
                .build();
        NotificationCategory givenNotificationCategory = new NotificationCategory("categoryId", notificationAction1, notificationAction2);
        Intent givenIntent = givenIntent(givenNotificationCategory, givenTriggeredActionId, givenNotificationId, notificationSettings.getIntentFlags());

        // When
        notificationActionReceiver.onReceive(contextMock, givenIntent);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(1)).cancel(givenNotificationId);
        Mockito.verify(broadcastSender, Mockito.times(1))
                .notificationActionTriggered(notificationCategoryArgumentCaptor.capture(), eq(givenTriggeredActionId));
        assertJEquals(givenNotificationCategory, notificationCategoryArgumentCaptor.getValue());

        Mockito.verify(contextMock, Mockito.times(1)).startActivity(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        String actualTriggeredActionId = intent.getStringExtra(BroadcastParameter.EXTRA_TRIGGERED_ACTION_ID);
        Bundle actualTriggeredCategoryBundle = intent.getBundleExtra(BroadcastParameter.EXTRA_TRIGGERED_CATEGORY);
        NotificationCategory notificationCategory = NotificationCategory.createFrom(actualTriggeredCategoryBundle);

        assertEquals(notificationSettings.getIntentFlags() | Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
        assertEquals(givenTriggeredActionId, actualTriggeredActionId);
        assertJEquals(givenNotificationCategory, notificationCategory);
    }

    @NonNull
    private NotificationAction.Builder givenNotificationAction(String givenTriggeredActionId) {
        return new NotificationAction.Builder()
                .withId(givenTriggeredActionId)
                .withIcon(android.R.drawable.btn_default)
                .withTitle("btn title");
    }

    private Intent givenIntent(NotificationCategory notificationCategory, String actionId, int notificationId, int flags) {
        return new Intent(context, NotificationActionReceiver.class)
                .putExtra(EXTRA_TRIGGERED_ACTION_ID, actionId)
                .putExtra(EXTRA_TRIGGERED_CATEGORY, NotificationCategoryBundleMapper.notificationCategoryToBundle(notificationCategory))
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .putExtra(EXTRA_INTENT_FLAGS.getKey(), flags);
    }
}
