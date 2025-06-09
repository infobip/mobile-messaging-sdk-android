package org.infobip.mobile.messaging.interactive.inapp.view;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppNativeCtx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

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
        InAppNativeView view = mock(InAppNativeView.class);
        Message message = mock(Message.class);
        NotificationAction[] actions = new NotificationAction[]{mock(NotificationAction.class)};
        NotificationCategory category = mock(NotificationCategory.class);
        InAppNativeCtx inAppCtx = new InAppNativeCtx(view, message, category, actions);

        queuedDialogStack.add(inAppCtx);

        verify(view, times(1)).show(eq(message), eq(category), eq(actions[0]));
    }

    @Test
    public void shouldNotShowDialogAgainIfClosed() {
        InAppNativeView view = mock(InAppViewDialog.class);
        Message message = mock(Message.class);
        NotificationAction[] actions = new NotificationAction[]{mock(NotificationAction.class)};
        NotificationCategory category = mock(NotificationCategory.class);
        InAppNativeCtx inAppCtx = new InAppNativeCtx(view, message, category, actions);

        queuedDialogStack.add(inAppCtx);
        queuedDialogStack.remove(view);

        verify(view, times(1)).show(eq(message), eq(category), eq(actions[0]));
    }

    @Test
    public void shouldShowDialogsInTheOrderOfAdditionToQueue() {
        InAppNativeView view1 = mock(InAppNativeView.class);
        InAppNativeView view2 = mock(InAppNativeView.class);
        InAppNativeView view3 = mock(InAppNativeView.class);
        Message message1 = mock(Message.class);
        Message message2 = mock(Message.class);
        Message message3 = mock(Message.class);
        NotificationAction[] actions1 = new NotificationAction[]{mock(NotificationAction.class)};
        NotificationAction[] actions2 = new NotificationAction[]{mock(NotificationAction.class)};
        NotificationAction[] actions3 = new NotificationAction[]{mock(NotificationAction.class)};
        NotificationCategory category1 = mock(NotificationCategory.class);
        NotificationCategory category2 = mock(NotificationCategory.class);
        NotificationCategory category3 = mock(NotificationCategory.class);
        InAppNativeCtx inAppCtx1 = new InAppNativeCtx(view1, message1, category1, actions1);
        InAppNativeCtx inAppCtx2 = new InAppNativeCtx(view2, message2, category2, actions2);
        InAppNativeCtx inAppCtx3 = new InAppNativeCtx(view3, message3, category3, actions3);

        queuedDialogStack.add(inAppCtx1);
        queuedDialogStack.add(inAppCtx2);
        queuedDialogStack.add(inAppCtx3);
        queuedDialogStack.remove(view1);
        queuedDialogStack.remove(view2);

        InOrder inOrder = inOrder(view1, view2, view3);
        inOrder.verify(view1, times(1)).show(eq(message1), eq(category1), eq(actions1[0]));
        inOrder.verify(view2, times(1)).show(eq(message2), eq(category2), eq(actions2[0]));
        inOrder.verify(view3, times(1)).show(eq(message3), eq(category3), eq(actions3[0]));
    }

    // TODO: test download task (mocking of async tasks is not done properly yet)
}
