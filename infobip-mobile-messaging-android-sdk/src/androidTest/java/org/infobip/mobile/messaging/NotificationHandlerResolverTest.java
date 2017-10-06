package org.infobip.mobile.messaging;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.notification.CoreNotificationHandler;
import org.infobip.mobile.messaging.notification.MockNotificationHandler;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.util.ModuleLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

/**
 * @author tjuric
 * @since 19/09/17.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationHandlerResolverTest {

    private ModuleLoader moduleLoaderMock;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        moduleLoaderMock = mock(ModuleLoader.class);
    }

    @Test
    public void should_get_core_notification_handler_when_no_handlers_in_manifest() throws InterruptedException {
        // Given
        given(moduleLoaderMock.loadModules(eq(NotificationHandler.class))).willReturn(new HashMap<String, NotificationHandler>());
        MobileMessagingCore givenCore = new MobileMessagingCore(context, new AndroidBroadcaster(context), Executors.newSingleThreadExecutor(), moduleLoaderMock);

        // When
        NotificationHandler notificationHandler = givenCore.getNotificationHandler();

        // Then
        assertTrue(notificationHandler instanceof CoreNotificationHandler);
    }

    @Test
    public void should_get_test_notification_handler() throws InterruptedException {
        // Given
        given(moduleLoaderMock.loadModules(eq(NotificationHandler.class))).willReturn(new HashMap<String, NotificationHandler>() {{
            put(MockNotificationHandler.class.getName(), new MockNotificationHandler());
        }});
        MobileMessagingCore givenCore = new MobileMessagingCore(context, new AndroidBroadcaster(context), Executors.newSingleThreadExecutor(), moduleLoaderMock);

        // When
        NotificationHandler notificationHandler = givenCore.getNotificationHandler();

        // Then
        assertFalse(notificationHandler instanceof CoreNotificationHandler);
        assertTrue(notificationHandler instanceof MockNotificationHandler);
    }

}
