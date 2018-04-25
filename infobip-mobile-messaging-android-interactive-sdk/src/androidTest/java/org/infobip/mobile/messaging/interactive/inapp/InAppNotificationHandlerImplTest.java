package org.infobip.mobile.messaging.interactive.inapp;

import android.app.Activity;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessageCache;
import org.infobip.mobile.messaging.interactive.inapp.rules.InAppRules;
import org.infobip.mobile.messaging.interactive.inapp.rules.ShowOrNot;
import org.infobip.mobile.messaging.interactive.inapp.view.DialogStack;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppView;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppViewFactory;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author sslavin
 * @since 18/04/2018.
 */
public class InAppNotificationHandlerImplTest {

    private InAppNotificationHandlerImpl inAppNotificationHandler;

    private MobileInteractive mobileInteractive = mock(MobileInteractive.class);
    private InAppViewFactory inAppViewFactory = mock(InAppViewFactory.class);
    private InAppRules inAppRules = mock(InAppRules.class);
    private OneMessageCache oneMessageCache = mock(OneMessageCache.class);
    private InAppView inAppView = mock(InAppView.class);
    private DialogStack dialogStack = mock(DialogStack.class);

    private Activity activity = mock(Activity.class);

    @Before
    public void before() {
        reset(mobileInteractive, inAppViewFactory, inAppRules, oneMessageCache, inAppView);
        when(inAppViewFactory.create(eq(activity), any(InAppView.Callback.class))).thenReturn(inAppView);
        inAppNotificationHandler = new InAppNotificationHandlerImpl(mobileInteractive, inAppViewFactory, inAppRules, oneMessageCache, dialogStack);
    }

    @Test
    public void shouldShowDialogWhenInForeground() {
        Message message = message();
        NotificationAction actions[] = actions();
        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(ShowOrNot.showNow(actions, activity));

        inAppNotificationHandler.handleMessage(message);

        verify(dialogStack, times(1)).add(eq(inAppView), eq(message), eq(actions));
    }

    @Test
    public void shouldShowMessageOnceWhenGoesToForeground() {
        Message message = message();
        NotificationAction actions[] = actions();
        when(oneMessageCache.getAndRemove()).thenReturn(message).thenReturn(null);
        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(ShowOrNot.showNow(actions, activity));

        inAppNotificationHandler.appWentToForeground();
        inAppNotificationHandler.appWentToForeground();

        verify(oneMessageCache, times(2)).getAndRemove();
        verify(dialogStack, times(1)).add(eq(inAppView), eq(message), eq(actions));
    }

    @Test
    public void shouldNotShowDialogWhenProhibitedByRules() {
        Message message = message();
        when(inAppRules.shouldDisplayDialogFor(any(Message.class))).thenReturn(ShowOrNot.not());

        inAppNotificationHandler.handleMessage(message);

        verify(inAppViewFactory, never()).create(eq(activity), eq(inAppNotificationHandler));
    }

    @Test
    public void shouldSaveMessageToCacheWhenInBackground() {
        Message message = message();
        when(inAppRules.shouldDisplayDialogFor(any(Message.class))).thenReturn(ShowOrNot.showWhenInForeground(actions()));

        inAppNotificationHandler.handleMessage(message);

        verify(inAppViewFactory, never()).create(eq(activity), eq(inAppNotificationHandler));
        verify(oneMessageCache, times(1)).save(eq(message));
        verifyNoMoreInteractions(dialogStack);
    }

    @Test
    public void shouldGetMessageFromCacheAndShowItWhenAppGoesToForeground() {
        Message message = message();
        NotificationAction actions[] = actions();
        when(oneMessageCache.getAndRemove()).thenReturn(message);
        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(ShowOrNot.showNow(actions, activity));

        inAppNotificationHandler.appWentToForeground();

        verify(oneMessageCache, times(1)).getAndRemove();
        verify(inAppViewFactory, times(1)).create(eq(activity), eq(inAppNotificationHandler));
        verify(dialogStack, times(1)).add(eq(inAppView), eq(message), eq(actions));
    }

    @Test
    public void shouldTriggerSdkActionsWhenButtonIsPressed() {
        Message message = message();
        NotificationAction action = actions()[0];

        inAppNotificationHandler.buttonPressedFor(inAppView, message, action);

        verify(mobileInteractive, times(1)).triggerSdkActionsFor(eq(action), eq(message));
    }

    @Test
    public void shouldRemoveMessageFromCacheIfButtonPressedFromNotification() {
        Message message = message();

        inAppNotificationHandler.userPressedNotificationButtonForMessage(message);

        verify(oneMessageCache, times(1)).remove(eq(message));
    }

    @Test
    public void shouldRemoveDialogFromStackWhenDismissed() {
        inAppNotificationHandler.dismissed(inAppView);

        verify(dialogStack, times(1)).remove(eq(inAppView));
    }

    @Test
    public void shouldClearDialogStackWhenEntersForeground() {
        inAppNotificationHandler.appWentToForeground();

        verify(dialogStack, times(1)).clear();
    }

    private NotificationAction[] actions() {
        return new NotificationAction[] { mock(NotificationAction.class) };
    }

    private Message message() {
        return new Message() {{
            setCategory("categoryId");
        }};
    }
}
