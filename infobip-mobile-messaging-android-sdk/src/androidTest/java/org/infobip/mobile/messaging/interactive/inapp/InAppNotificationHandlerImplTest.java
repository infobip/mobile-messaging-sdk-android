package org.infobip.mobile.messaging.interactive.inapp;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Intent;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.app.ActivityStarterWrapper;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessageCache;
import org.infobip.mobile.messaging.interactive.inapp.rules.InAppRules;
import org.infobip.mobile.messaging.interactive.inapp.rules.ShowOrNot;
import org.infobip.mobile.messaging.interactive.inapp.view.DialogStack;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppNativeView;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppView;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppViewFactory;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppWebView;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppNativeCtx;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppWebCtx;
import org.infobip.mobile.messaging.interactive.platform.InteractiveBroadcaster;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
    private InAppNativeView inAppNativeView = mock(InAppNativeView.class);
    private InAppWebView inAppWebView = mock(InAppWebView.class);
    private DialogStack dialogStack = mock(DialogStack.class);
    private InteractiveBroadcaster interactiveBroadcaster = mock(InteractiveBroadcaster.class);
    private ActivityStarterWrapper activityStarterWrapper = mock(ActivityStarterWrapper.class);

    private Activity activity = mock(Activity.class);

    @Before
    public void before() {
        reset(mobileInteractive, inAppViewFactory, inAppRules, oneMessageCache, inAppView);
        inAppNotificationHandler = Mockito.spy(new InAppNotificationHandlerImpl(mobileInteractive, inAppViewFactory, inAppRules, oneMessageCache, dialogStack, interactiveBroadcaster, activityStarterWrapper));
    }

    @Test
    public void testThatInAppWebViewMessageIsNotNullWhenEverythingIsProvided() {
        InAppWebViewMessage inAppWebViewMessage = inAppWebViewMessage(InAppWebViewMessage.InAppWebViewPosition.TOP, InAppWebViewMessage.InAppWebViewType.BANNER);
        assertEquals(InAppWebViewMessage.InAppWebViewType.BANNER, inAppWebViewMessage.type);
        assertEquals(InAppWebViewMessage.InAppWebViewPosition.TOP, inAppWebViewMessage.position);
        assertEquals("http://google.com", inAppWebViewMessage.url);
    }

    @Test
    public void testThatInAppWebViewMessageIsNullWhenInternalDataIsMissing() {
        Message message = new Message();
        assertNull(InAppWebViewMessage.createInAppWebViewMessage(message));
    }

    @Test
    public void testThatInAppWebViewMessageIsNullWhenInAppDetailsIsMissing() {
        Message message = new Message() {{
            setInternalData("");
        }};
        assertNull(InAppWebViewMessage.createInAppWebViewMessage(message));
    }

    @Test
    public void testThatInAppWebViewMessageIsNullWhenTypeIsWrong() {
        Message message = new Message() {{
            setInternalData("{\"inAppDetails\":{\"url\":\"http://google.com\",\"position\":0,\"type\":3}}");
        }};
        assertNull(InAppWebViewMessage.createInAppWebViewMessage(message));
    }

    @Test
    public void testThatInAppWebViewMessagePositionIsSetToTopWhenBannerPositionIsMissing() {
        Message message = new Message() {{
            setInternalData("{\"inAppDetails\":{\"url\":\"http://google.com\",\"type\":0}}");
        }};
        InAppWebViewMessage inAppWebViewMessage = InAppWebViewMessage.createInAppWebViewMessage(message);
        assertEquals(InAppWebViewMessage.InAppWebViewPosition.TOP, inAppWebViewMessage.position);
    }

    @Test
    public void shouldShowInAppNativeDialogWhenInForeground() {
        Message message = message();
        NotificationAction[] actions = actions();
        NotificationCategory category = category(message.getCategory(), actions);
        ShowOrNot showOrNot = ShowOrNot.showNow(category, actions, activity);

        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(showOrNot);
        when(inAppRules.areModalInAppNotificationsEnabled()).thenReturn(true);
        when(inAppViewFactory.create(eq(activity), any(InAppView.Callback.class), eq(message))).thenReturn(inAppNativeView);


        inAppNotificationHandler.handleMessage(message);

        verify(dialogStack, times(1)).add(any(InAppNativeCtx.class));
    }

    @Test
    public void shouldShowInAppWebViewDialogWhenInForeground() {
        InAppWebViewMessage webViewMessage = inAppWebViewMessage(InAppWebViewMessage.InAppWebViewPosition.TOP, InAppWebViewMessage.InAppWebViewType.BANNER);
        NotificationAction[] actions = actions();
        ShowOrNot showOrNot = ShowOrNot.showNow(null, actions, activity);

        when(inAppRules.shouldDisplayDialogFor(any(InAppWebViewMessage.class))).thenReturn(showOrNot);
        when(inAppRules.areModalInAppNotificationsEnabled()).thenReturn(true);
        when(inAppViewFactory.create(eq(activity), any(InAppView.Callback.class), eq(webViewMessage))).thenReturn(inAppWebView);

        inAppNotificationHandler.handleMessage(webViewMessage);

        verify(dialogStack, times(1)).add(any(InAppWebCtx.class));
    }

    @Test
    public void shouldShowNativeMessageOnceWhenGoesToForeground() {
        Message message = message();
        NotificationAction[] actions = actions();
        NotificationCategory category = category(message.getCategory(), actions);
        ShowOrNot showOrNot = ShowOrNot.showNow(category, actions, activity);
        when(oneMessageCache.getAndRemove()).thenReturn(message).thenReturn(null);
        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(showOrNot);
        when(inAppRules.areModalInAppNotificationsEnabled()).thenReturn(true);
        when(inAppViewFactory.create(eq(activity), any(InAppView.Callback.class), eq(message))).thenReturn(inAppNativeView);

        inAppNotificationHandler.appWentToForeground();
        inAppNotificationHandler.appWentToForeground();

        verify(oneMessageCache, times(2)).getAndRemove();
        verify(dialogStack, times(1)).add(any(InAppNativeCtx.class));
    }

    @Test
    public void shouldShowWebViewMessageOnceWhenGoesToForeground() {
        InAppWebViewMessage webViewMessage = inAppWebViewMessage(InAppWebViewMessage.InAppWebViewPosition.TOP, InAppWebViewMessage.InAppWebViewType.BANNER);
        NotificationAction[] actions = actions();
        ShowOrNot showOrNot = ShowOrNot.showNow(null, actions, activity);
        when(oneMessageCache.getAndRemove()).thenReturn(webViewMessage).thenReturn(null);
        when(inAppRules.shouldDisplayDialogFor(any(InAppWebViewMessage.class))).thenReturn(showOrNot);
        when(inAppRules.areModalInAppNotificationsEnabled()).thenReturn(true);
        when(inAppViewFactory.create(eq(activity), any(InAppView.Callback.class), eq(webViewMessage))).thenReturn(inAppWebView);


        inAppNotificationHandler.appWentToForeground();
        inAppNotificationHandler.appWentToForeground();

        verify(oneMessageCache, times(2)).getAndRemove();
        verify(dialogStack, times(1)).add(any(InAppWebCtx.class));
    }

    @Test
    public void shouldNotShowDialogWhenProhibitedByRules() {
        Message message = message();
        when(inAppRules.shouldDisplayDialogFor(any(Message.class))).thenReturn(ShowOrNot.not());
        when(inAppRules.areModalInAppNotificationsEnabled()).thenReturn(true);

        inAppNotificationHandler.handleMessage(message);

        verify(inAppViewFactory, never()).create(eq(activity), eq(inAppNotificationHandler), eq(message));
    }

    @Test
    public void shouldSaveMessageToCacheWhenInBackground() {
        Message message = message();
        when(inAppRules.shouldDisplayDialogFor(any(Message.class))).thenReturn(ShowOrNot.showWhenInForeground());
        when(inAppRules.areModalInAppNotificationsEnabled()).thenReturn(true);

        inAppNotificationHandler.handleMessage(message);

        verify(inAppViewFactory, never()).create(eq(activity), eq(inAppNotificationHandler), eq(message));
        verify(oneMessageCache, times(1)).save(eq(message));
        verifyNoMoreInteractions(dialogStack);
    }

    @Test
    public void shouldGetMessageFromCacheAndShowItWhenAppGoesToForeground() {
        Message message = message();
        NotificationAction[] actions = actions();
        NotificationCategory category = category(message.getCategory(), actions);
        ShowOrNot showOrNot = ShowOrNot.showNow(category, actions, activity);
        when(oneMessageCache.getAndRemove()).thenReturn(message);
        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(showOrNot);
        when(inAppRules.areModalInAppNotificationsEnabled()).thenReturn(true);
        when(inAppViewFactory.create(eq(activity), any(InAppView.Callback.class), eq(message))).thenReturn(inAppNativeView);

        inAppNotificationHandler.appWentToForeground();

        verify(oneMessageCache, times(1)).getAndRemove();
        verify(inAppViewFactory, times(1)).create(eq(activity), eq(inAppNotificationHandler), eq(message));
        verify(dialogStack, times(1)).add(any(InAppNativeCtx.class));
    }

    @Test
    public void shouldTriggerSdkActionsAndBroadcastWhenButtonIsPressed() {
        Message message = message();
        NotificationAction[] actions = actions();
        NotificationCategory category = category(message.getCategory(), actions);

        inAppNotificationHandler.buttonPressedFor(inAppNativeView, message, category, actions[0]);

        verify(mobileInteractive, times(1)).triggerSdkActionsFor(eq(actions[0]), eq(message));
        verify(interactiveBroadcaster, times(1)).notificationActionTapped(eq(message), eq(category), eq(actions[0]));
    }

    @Test
    public void shouldStartCallbackActivityIfActionShouldBringAppToForegroundWhenButtonIsPressed() {
        Message message = message();
        NotificationAction[] actions = new NotificationAction[]{new NotificationAction.Builder()
                .withBringingAppToForeground(true)
                .withId("id")
                .withTitleResourceId(1)
                .build()};
        NotificationCategory category = category(message.getCategory(), actions);

        inAppNotificationHandler.buttonPressedFor(inAppNativeView, message, category, actions[0]);

        assertTrue(actions[0].bringsAppToForeground());
        verify(activityStarterWrapper, times(1)).startCallbackActivity(any(Intent.class));
    }

    @Test
    public void shouldNotStartCallbackActivityIfActionShouldNotBringAppToForegroundWhenButtonIsPressed() {
        Message message = message();
        NotificationAction[] actions = actions();
        NotificationCategory category = category(message.getCategory(), actions);

        inAppNotificationHandler.buttonPressedFor(inAppNativeView, message, category, actions[0]);

        assertFalse(actions[0].bringsAppToForeground());
        verify(activityStarterWrapper, never()).startCallbackActivity(any(Intent.class));
    }

    @Test
    public void shouldTriggerSdkActionsAndBroadcastAndStartCallbackActivityWhenOpenButtonIsPressed() {
        Message message = message();
        NotificationAction action = new NotificationAction.Builder(true).withId("mm_open").withTitleText("Open").build();
        NotificationCategory category = category(message.getCategory(), action);

        inAppNotificationHandler.buttonPressedFor(inAppNativeView, message, category, action);

        verify(mobileInteractive, times(1)).triggerSdkActionsFor(eq(action), eq(message));
        verify(interactiveBroadcaster, times(1)).notificationActionTapped(eq(message), eq(category), eq(action));
        verify(activityStarterWrapper, times(1)).startCallbackActivity(any(Intent.class));
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

    @Test
    public void shouldSendBroadcastNotificationIfInAppsDisabled() {
        Message message = message();
        NotificationAction[] actions = actions();
        NotificationCategory category = category(message.getCategory(), actions);

        when(inAppRules.shouldDisplayDialogFor(eq(message))).thenReturn(ShowOrNot.showNow(category, actions, activity));
        when(inAppRules.areModalInAppNotificationsEnabled()).thenReturn(false);

        inAppNotificationHandler.handleMessage(message);

        verify(interactiveBroadcaster, times(1)).inAppNotificationIsReadyToDisplay(message);
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

    private InAppWebViewMessage inAppWebViewMessage(InAppWebViewMessage.InAppWebViewPosition inAppPosition, InAppWebViewMessage.InAppWebViewType inAppType) {
        return new InAppWebViewMessage() {{
            position = inAppPosition;
            type = inAppType;
            url = "http://google.com";
            setInternalData(String.format("{\"inAppDetails\":{\"url\":\"http://google.com\",\"position\":%d,\"type\":%d}}", inAppPosition.ordinal(), inAppType.ordinal()));
        }};
    }
}
