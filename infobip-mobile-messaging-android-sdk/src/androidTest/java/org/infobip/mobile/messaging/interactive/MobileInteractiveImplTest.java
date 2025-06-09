package org.infobip.mobile.messaging.interactive;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.interactive.inapp.InAppNotificationHandler;
import org.infobip.mobile.messaging.interactive.predefined.PredefinedActionsProvider;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 18/04/2018.
 */
public class MobileInteractiveImplTest {

    private MessageHandlerModule mobileInteractive;

    private InAppNotificationHandler inAppNotificationHandler = mock(InAppNotificationHandler.class);

    // TODO: test remaining features and migrate tests from NotificationCategoriesTest

    @Before
    public void before() {
        reset(inAppNotificationHandler);
        mobileInteractive = new MobileInteractiveImpl(mock(Context.class), mock(MobileMessagingCore.class), inAppNotificationHandler, mock(PredefinedActionsProvider.class));
    }

    @Test
    public void shouldCallInAppHandlerWhenMessageReceived() {
        Message message = message();

        mobileInteractive.handleMessage(message);

        verify(inAppNotificationHandler, times(1)).handleMessage(eq(message));
    }

    @Test
    public void shouldInvokeInAppHandlerWhenGoesToForeground() {
        mobileInteractive.applicationInForeground();

        verify(inAppNotificationHandler, times(1)).appWentToForeground();
    }

    private Message message() {
        return new Message();
    }
}
