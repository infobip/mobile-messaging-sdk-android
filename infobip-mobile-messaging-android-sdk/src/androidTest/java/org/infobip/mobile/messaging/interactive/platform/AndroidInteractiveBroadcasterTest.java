package org.infobip.mobile.messaging.interactive.platform;

import android.content.Intent;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.InteractiveEvent;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static android.R.id.message;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;


public class AndroidInteractiveBroadcasterTest extends MobileMessagingTestCase {

    private AndroidInteractiveBroadcaster broadcastSender;
    private ArgumentCaptor<Intent> intentArgumentCaptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = new AndroidInteractiveBroadcaster(contextMock);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    }

    @Test
    public void test_should_send_notification_action_tapped_event() throws Exception {
        // Given
        Message givenMessage = createMessage(context, "SomeMessageId", false);
        NotificationAction notificationAction = givenNotificationAction("actionIdNotTapped").build();
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId").build();
        NotificationCategory givenNotificationCategory = new NotificationCategory("categoryId", notificationAction, givenTappedNotificationAction);

        // When
        Intent actionTappedIntent = broadcastSender.notificationActionTapped(givenMessage, givenNotificationCategory, givenTappedNotificationAction);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent sentBroadcastIntent = intentArgumentCaptor.getValue();
        assertEquals(InteractiveEvent.NOTIFICATION_ACTION_TAPPED.getKey(), sentBroadcastIntent.getAction());
        assertEquals(actionTappedIntent, sentBroadcastIntent);

        Message messageAfter = Message.createFrom(sentBroadcastIntent.getExtras());
        NotificationAction actionAfter = NotificationAction.createFrom(sentBroadcastIntent.getExtras());
        NotificationCategory categoryAfter = NotificationCategory.createFrom(sentBroadcastIntent.getExtras());
        assertNotSame(message, messageAfter);
        assertEquals("SomeMessageId", messageAfter.getMessageId());
        assertJEquals(givenMessage, messageAfter);
        assertJEquals(givenTappedNotificationAction, actionAfter);
        assertJEquals(givenNotificationCategory, categoryAfter);
    }
}
