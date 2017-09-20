package org.infobip.mobile.messaging;

import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.notification.CoreNotificationHandler;
import org.infobip.mobile.messaging.notification.MockNotificationHandler;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * @author tjuric
 * @since 19/09/17.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationHandlerResolverTest {

    private MobileMessagingCore mmcMock = mock(MobileMessagingCore.class);

    @Before
    public void setUp() throws Exception {
        reset(mmcMock);
    }

    @Test
    public void should_get_core_notification_handler_when_invalid_classname_provided() throws InterruptedException {
        // Given
        String givenClassName = "some_invalid_class_name";

        // When
        NotificationHandler notificationHandler = mmcMock.getNotificationHandler(givenClassName);

        // Then
        assertTrue(notificationHandler instanceof CoreNotificationHandler);
    }

    @Test
    public void should_get_core_notification_handler_when_classname_absent() throws InterruptedException {
        // When
        NotificationHandler notificationHandler = mmcMock.getNotificationHandler(null);

        // Then
        assertTrue(notificationHandler instanceof CoreNotificationHandler);
    }

    @Test
    public void should_get_test_notification_handler() throws InterruptedException {
        // Given
        String givenClassName = MockNotificationHandler.class.getCanonicalName();

        // When
        NotificationHandler notificationHandler = mmcMock.getNotificationHandler(givenClassName);

        // Then
        assertFalse(notificationHandler instanceof CoreNotificationHandler);
        assertTrue(notificationHandler instanceof MockNotificationHandler);
    }

}
