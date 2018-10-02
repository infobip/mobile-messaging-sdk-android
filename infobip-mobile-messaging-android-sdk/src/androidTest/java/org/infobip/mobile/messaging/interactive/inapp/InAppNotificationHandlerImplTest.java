package org.infobip.mobile.messaging.interactive.inapp;

import android.app.Activity;
import android.content.Intent;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.app.CallbackActivityStarterWrapper;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessageCache;
import org.infobip.mobile.messaging.interactive.inapp.rules.InAppRules;
import org.infobip.mobile.messaging.interactive.inapp.rules.ShowOrNot;
import org.infobip.mobile.messaging.interactive.inapp.view.DialogStack;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppView;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppViewFactory;
import org.infobip.mobile.messaging.interactive.platform.InteractiveBroadcaster;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
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
    private InteractiveBroadcaster interactiveBroadcaster = mock(InteractiveBroadcaster.class);
    private CallbackActivityStarterWrapper callbackActivityStarterWrapper = mock(CallbackActivityStarterWrapper.class);

    private Activity activity = mock(Activity.class);

    @Before
    public void before() {
        reset(mobileInteractive, inAppViewFactory, inAppRules, oneMessageCache, inAppView);
        when(inAppViewFactory.create(eq(activity), any(InAppView.Callback.class))).thenReturn(inAppView);
        inAppNotificationHandler = new InAppNotificationHandlerImpl(mobileInteractive, inAppViewFactory, inAppRules, oneMessageCache, dialogStack, interactiveBroadcaster, callbackActivityStarterWrapper);
    }

    @Test
    public void shouldShowDialogWhenInForeground() {
        Message message = message();
        NotificationAction actions[] = actions();
        NotificationCategory category = category(message.getCategory(), actions);

        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(ShowOrNot.showNow(category, actions, activity));

        inAppNotificationHandler.handleMessage(message);

        verify(dialogStack, times(1)).add(eq(inAppView), eq(message), eq(category), eq(actions));
    }

    @Test
    public void shouldShowMessageOnceWhenGoesToForeground() {
        Message message = message();
        NotificationAction actions[] = actions();
        NotificationCategory category = category(message.getCategory(), actions);
        when(oneMessageCache.getAndRemove()).thenReturn(message).thenReturn(null);
        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(ShowOrNot.showNow(category, actions, activity));

        inAppNotificationHandler.appWentToForeground();
        inAppNotificationHandler.appWentToForeground();

        verify(oneMessageCache, times(2)).getAndRemove();
        verify(dialogStack, times(1)).add(eq(inAppView), eq(message), eq(category), eq(actions));
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
        when(inAppRules.shouldDisplayDialogFor(any(Message.class))).thenReturn(ShowOrNot.showWhenInForeground());

        inAppNotificationHandler.handleMessage(message);

        verify(inAppViewFactory, never()).create(eq(activity), eq(inAppNotificationHandler));
        verify(oneMessageCache, times(1)).save(eq(message));
        verifyNoMoreInteractions(dialogStack);
    }

    @Test
    public void shouldGetMessageFromCacheAndShowItWhenAppGoesToForeground() {
        Message message = message();
        NotificationAction actions[] = actions();
        NotificationCategory category = category(message.getCategory(), actions);
        when(oneMessageCache.getAndRemove()).thenReturn(message);
        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(ShowOrNot.showNow(category, actions, activity));

        inAppNotificationHandler.appWentToForeground();

        verify(oneMessageCache, times(1)).getAndRemove();
        verify(inAppViewFactory, times(1)).create(eq(activity), eq(inAppNotificationHandler));
        verify(dialogStack, times(1)).add(eq(inAppView), eq(message), eq(category), eq(actions));
    }

    @Test
    public void shouldTriggerSdkActionsAndBroadcastWhenButtonIsPressed() {
        Message message = message();
        NotificationAction actions[] = actions();
        NotificationCategory category = category(message.getCategory(), actions);

        inAppNotificationHandler.buttonPressedFor(inAppView, message, category, actions[0]);

        verify(mobileInteractive, times(1)).triggerSdkActionsFor(eq(actions[0]), eq(message));
        verify(interactiveBroadcaster, times(1)).notificationActionTapped(eq(message), eq(category), eq(actions[0]));
    }

    @Test
    public void shouldStartCallbackActivityIfActionShouldBringAppToForegroundWhenButtonIsPressed() {
        Message message = message();
        NotificationAction actions[] = new NotificationAction[]{new NotificationAction.Builder()
                .withBringingAppToForeground(true)
                .withId("id")
                .withTitleResourceId(1)
                .build()};
        NotificationCategory category = category(message.getCategory(), actions);

        inAppNotificationHandler.buttonPressedFor(inAppView, message, category, actions[0]);

        assertEquals(true, actions[0].bringsAppToForeground());
        verify(callbackActivityStarterWrapper, times(1)).startActivity(any(Intent.class));
    }

    @Test
    public void shouldNotStartCallbackActivityIfActionShouldNotBringAppToForegroundWhenButtonIsPressed() {
        Message message = message();
        NotificationAction actions[] = actions();
        NotificationCategory category = category(message.getCategory(), actions);

        inAppNotificationHandler.buttonPressedFor(inAppView, message, category, actions[0]);

        assertEquals(false, actions[0].bringsAppToForeground());
        verify(callbackActivityStarterWrapper, never()).startActivity(any(Intent.class));
    }

    @Test
    public void shouldTriggerSdkActionsAndBroadcastAndStartCallbackActivityWhenOpenButtonIsPressed() {
        Message message = message();
        NotificationAction action = new NotificationAction.Builder(true).withId("mm_open").withTitleText("Open").build();
        NotificationCategory category = category(message.getCategory(), action);

        inAppNotificationHandler.buttonPressedFor(inAppView, message, category, action);

        verify(mobileInteractive, times(1)).triggerSdkActionsFor(eq(action), eq(message));
        verify(interactiveBroadcaster, times(1)).notificationActionTapped(eq(message), eq(category), eq(action));
        verify(callbackActivityStarterWrapper, times(1)).startActivity(any(Intent.class));
    }

    @Test
    public void shouldRemoveMessageFromCacheIfButtonPressedFromNotification() {
        Message message = message();

        inAppNotificationHandler.userPressedNotificationButtonForMessage(message);

        verify(oneMessageCache, times(1)).remove(eq(message));
    }

    @Test
    public void shouldRemoveMessageFromCacheIfMessageWithoutCategoryWasTappedFromNotification() {
        Message message = message();
        message.setCategory(null);

        inAppNotificationHandler.userTappedNotificationForMessage(message);

        verify(oneMessageCache, times(1)).remove(eq(message));
    }

    @Test
    public void shouldLeaveMessageInCacheIfMessageWithCategoryWasTappedFromNotification() {
        Message message = message();

        inAppNotificationHandler.userTappedNotificationForMessage(message);

        verify(oneMessageCache, never()).remove(eq(message));
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

    private NotificationCategory category(String categoryId, NotificationAction... actions) {
        return new NotificationCategory(categoryId, actions);
    }

    private NotificationAction[] actions() {
        return new NotificationAction[]{mock(NotificationAction.class)};
    }

    private Message message() {
        return new Message() {{
            setCategory("categoryId");
        }};
    }
}
