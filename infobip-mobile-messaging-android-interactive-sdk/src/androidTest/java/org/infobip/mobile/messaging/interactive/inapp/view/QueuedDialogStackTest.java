package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 25/04/2018.
 */
public class QueuedDialogStackTest {

    private QueuedDialogStack queuedDialogStack;

    @Before
    public void before() {
        queuedDialogStack = new QueuedDialogStack();
    }

    @Test
    public void shouldShowDialog() {
        InAppView view = mock(InAppView.class);
        Message message = mock(Message.class);
        NotificationAction actions[] = new NotificationAction[]{mock(NotificationAction.class)};

        queuedDialogStack.add(view, message, actions);

        verify(view, times(1)).show(eq(message), eq(actions[0]));
    }

    @Test
    public void shouldNotShowDialogAgainIfClosed() {
        InAppView view = mock(InAppView.class);
        Message message = mock(Message.class);
        NotificationAction actions[] = new NotificationAction[]{mock(NotificationAction.class)};

        queuedDialogStack.add(view, message, actions);
        queuedDialogStack.remove(view);

        verify(view, times(1)).show(eq(message), eq(actions[0]));
    }

    @Test
    public void shouldShowDialogsInTheOrderOfAdditionToQueue() {
        InAppView view1 = mock(InAppView.class);
        InAppView view2 = mock(InAppView.class);
        InAppView view3 = mock(InAppView.class);
        Message message1 = mock(Message.class);
        Message message2 = mock(Message.class);
        Message message3 = mock(Message.class);
        NotificationAction actions1[] = new NotificationAction[]{mock(NotificationAction.class)};
        NotificationAction actions2[] = new NotificationAction[]{mock(NotificationAction.class)};
        NotificationAction actions3[] = new NotificationAction[]{mock(NotificationAction.class)};

        queuedDialogStack.add(view1, message1, actions1);
        queuedDialogStack.add(view2, message2, actions2);
        queuedDialogStack.add(view3, message3, actions3);
        queuedDialogStack.remove(view1);
        queuedDialogStack.remove(view2);

        InOrder inOrder = inOrder(view1, view2, view3);
        inOrder.verify(view1, times(1)).show(eq(message1), eq(actions1[0]));
        inOrder.verify(view2, times(1)).show(eq(message2), eq(actions2[0]));
        inOrder.verify(view3, times(1)).show(eq(message3), eq(actions3[0]));
    }

    // TODO: test download task (mocking of async tasks is not done properly yet)
}
