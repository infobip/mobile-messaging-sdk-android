package org.infobip.mobile.messaging.interactive.inapp.view;

import android.content.DialogInterface;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 18/04/2018.
 */
public class InAppViewDialogClickListenerTest {

    private InAppViewDialogClickListener inAppViewDialogClickListener;

    private InAppView inAppView = mock(InAppView.class);
    private InAppView.Callback callback = mock(InAppView.Callback.class);
    private Message message = mock(Message.class);
    private NotificationAction notificationAction = mock(NotificationAction.class);

    @Before
    public void before() {
        reset(callback, message, inAppView, notificationAction);
        inAppViewDialogClickListener = new InAppViewDialogClickListener(inAppView, callback, message, notificationAction);
    }

    @Test
    public void shouldCallCallbackOnClick() {
        inAppViewDialogClickListener.onClick(mock(DialogInterface.class), 0);

        verify(callback, times(1)).buttonPressedFor(eq(inAppView), eq(message), eq(notificationAction));
    }
}
