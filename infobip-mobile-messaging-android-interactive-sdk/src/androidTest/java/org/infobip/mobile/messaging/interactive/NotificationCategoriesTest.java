package org.infobip.mobile.messaging.interactive;


import org.infobip.mobile.messaging.interactive.tools.MobileMessagingTestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class NotificationCategoriesTest extends MobileMessagingTestCase {

    private MobileInteractiveImpl mobileInteractive;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mobileInteractive = MobileInteractiveImpl.getInstance(context);
    }

    @Test
    public void shouldReturnCustomWithPredefinedCategories_whenGettingInteractiveCategories() throws Exception {
        //given
        NotificationCategory[] givenCustomNotificationCategories = new NotificationCategory[]{givenCategory("a1", "a2", "category")};
        Set<NotificationCategory> predefinedNotificationCategories = PredefinedNotificationCategories.load();

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
        Set<NotificationCategory> predefinedNotificationCategories = PredefinedNotificationCategories.load();
        mobileInteractive.setCustomNotificationCategories(new NotificationCategory[0]);

        //when
        Set<NotificationCategory> interactiveNotificationCategories = mobileInteractive.getNotificationCategories();

        //then
        int expectedInteractiveCategoriesSize = predefinedNotificationCategories.size();

        assertEquals(expectedInteractiveCategoriesSize, interactiveNotificationCategories.size());
        assertJEquals(predefinedNotificationCategories, interactiveNotificationCategories);
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
