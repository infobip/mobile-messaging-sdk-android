package org.infobip.mobile.messaging.interactive;


import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.interactive.inapp.InAppNotificationHandler;
import org.infobip.mobile.messaging.interactive.platform.MockActivity;
import org.infobip.mobile.messaging.interactive.predefined.PredefinedActionsProvider;
import org.infobip.mobile.messaging.interactive.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class NotificationCategoriesTest extends MobileMessagingTestCase {

    private MobileInteractiveImpl mobileInteractive;
    private MobileMessagingCore mmcMock;
    private InAppNotificationHandler inAppNotificationHandlerMock;
    private PredefinedActionsProvider predefinedActionsProvider;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private ArgumentCaptor<String> messageIdArgumentCaptor;
    private Set<NotificationCategory> predefinedNotificationCategories;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mmcMock = mock(MobileMessagingCore.class);
        inAppNotificationHandlerMock = mock(InAppNotificationHandler.class);
        predefinedActionsProvider = mock(PredefinedActionsProvider.class);
        mobileInteractive = new MobileInteractiveImpl(contextMock, mmcMock, inAppNotificationHandlerMock, predefinedActionsProvider);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        messageIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
        predefinedNotificationCategories = new HashSet<>();
        predefinedNotificationCategories.add(givenCategory("p1", "p2", "predefined_category"));

        NotificationSettings notificationSettings = new NotificationSettings.Builder(context)
                .withDefaultIcon(android.R.drawable.ic_dialog_alert) // if not set throws -> IllegalArgumentException("defaultIcon doesn't exist");
                .withCallbackActivity(MockActivity.class)
                .build();
        Mockito.when(mmcMock.getNotificationSettings()).thenReturn(notificationSettings);
        Mockito.when(predefinedActionsProvider.getPredefinedCategories()).thenReturn(predefinedNotificationCategories);
    }

    @Test
    public void shouldReturnCustomWithPredefinedCategories_whenGettingInteractiveCategories() throws Exception {
        //given
        NotificationCategory[] givenCustomNotificationCategories = new NotificationCategory[]{givenCategory("a1", "a2", "category")};

        Set<NotificationCategory> givenInteractiveCategories = new HashSet<>();
        givenInteractiveCategories.addAll(predefinedNotificationCategories);
        givenInteractiveCategories.addAll(Arrays.asList(givenCustomNotificationCategories));

        mobileInteractive.setCustomNotificationCategories(givenCustomNotificationCategories);

        //when
        Set<NotificationCategory> interactiveNotificationCategories = mobileInteractive.getNotificationCategories();

        //then
        assertEquals(givenInteractiveCategories.size(), interactiveNotificationCategories.size());
        assertTrue(interactiveNotificationCategories.contains(givenCustomNotificationCategories[0]));
    }

    @Test
    public void shouldReturnOnlyPredefinedCategories_whenGettingInteractiveCategoriesWithoutCustomCategories() throws Exception {
        //given
        mobileInteractive.setCustomNotificationCategories(new NotificationCategory[0]);

        //when
        Set<NotificationCategory> interactiveNotificationCategories = mobileInteractive.getNotificationCategories();

        //then
        int expectedInteractiveCategoriesSize = predefinedNotificationCategories.size();

        assertEquals(expectedInteractiveCategoriesSize, interactiveNotificationCategories.size());
        assertJEquals(predefinedNotificationCategories, interactiveNotificationCategories);
    }

    @Test
    public void shouldPerformSendMoAndMarkMessagesSeen_whenTriggeringSdkActions() throws Exception {
        //given
        NotificationAction givenTappedNotificationAction = givenNotificationAction("actionId")
                .withMoMessage()
                .build();
        NotificationCategory givenCategory = givenNotificationCategory(givenTappedNotificationAction);
        Message givenMessage = createMessage(context, "SomeMessageId", givenCategory.getCategoryId(), false);

        //when
        mobileInteractive.triggerSdkActionsFor(givenTappedNotificationAction, givenMessage);

        //then
        Mockito.verify(mmcMock, Mockito.times(1)).setMessagesSeen(messageIdArgumentCaptor.capture());
        Mockito.verify(mmcMock, Mockito.times(1)).sendMessagesWithRetry(messageArgumentCaptor.capture());

        Message actualMessage = messageArgumentCaptor.getValue();
        assertEquals(givenCategory.getCategoryId() + " " + givenTappedNotificationAction.getId(), actualMessage.getBody());
        assertEquals(givenMessage.getMessageId(), InternalDataMapper.getInternalDataInitialMessageId(actualMessage.getInternalData()));
        assertEquals(givenMessage.getMessageId(), messageIdArgumentCaptor.getValue());
    }

    private NotificationCategory givenCategory(String actionId1, String actionId2, String categoryId) {
        final NotificationAction mmDecline = new NotificationAction.Builder(false)
                .withId(actionId1)
                .withIcon(android.R.drawable.btn_default)
                .withTitleResourceId(android.R.string.no)
                .build();

        final NotificationAction mmAccept = new NotificationAction.Builder(false)
                .withId(actionId2)
                .withIcon(android.R.drawable.btn_default)
                .withTitleResourceId(android.R.string.ok)
                .withBringingAppToForeground(true)
                .build();

        return new NotificationCategory(false, categoryId, mmDecline, mmAccept);
    }
}
