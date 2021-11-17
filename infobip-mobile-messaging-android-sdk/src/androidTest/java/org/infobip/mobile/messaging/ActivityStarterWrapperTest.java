package org.infobip.mobile.messaging;


import android.app.Activity;
import android.content.Intent;

import org.infobip.mobile.messaging.app.ActivityStarterWrapper;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class ActivityStarterWrapperTest extends MobileMessagingTestCase {

    private ArgumentCaptor<Intent> intentArgumentCaptor;
    private ActivityStarterWrapper activityStarterWrapper;
    private NotificationSettings notificationSettings;
    private int givenFlags = Intent.FLAG_ACTIVITY_NO_HISTORY;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MobileMessagingCore mobileMessagingCore = mock(MobileMessagingCore.class);
        notificationSettings = new NotificationSettings(contextMock);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        activityStarterWrapper = new ActivityStarterWrapper(contextMock, mobileMessagingCore);

        PreferenceHelper.saveInt(context, MobileMessagingProperty.INTENT_FLAGS, givenFlags);
        PreferenceHelper.saveClass(contextMock, MobileMessagingProperty.CALLBACK_ACTIVITY, Activity.class);
        Mockito.when(mobileMessagingCore.getNotificationSettings()).thenReturn(notificationSettings);
    }

    @Test
    public void testShouldStartActivityWithActionCalled() throws Exception {

        // Given
        Intent givenIntent = new Intent();

        // When
        activityStarterWrapper.startCallbackActivity(givenIntent);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).startActivity(intentArgumentCaptor.capture());
        Intent calledIntent = intentArgumentCaptor.getValue();
        int expectedFlags = givenFlags | Intent.FLAG_ACTIVITY_NEW_TASK;

        assertEquals(expectedFlags, calledIntent.getFlags());
        assertNotNull(calledIntent.getComponent());
        assertEquals(notificationSettings.getCallbackActivity().getCanonicalName(), calledIntent.getComponent().getClassName());
    }

    @Test
    public void testShouldNotStartActivityIfCallbackActivityDoesNotExist() throws Exception {

        // Given
        PreferenceHelper.remove(contextMock, MobileMessagingProperty.CALLBACK_ACTIVITY);
        Intent givenIntent = new Intent();

        // When
        activityStarterWrapper.startCallbackActivity(givenIntent);

        // Then
        Mockito.verify(contextMock, Mockito.never()).startActivity(givenIntent);
    }

    @Test
    public void testShouldCreateContentIntentWithMessageAndFlags() throws Exception {
        // Given
        Message givenMessage = createMessage(context, "SomeMessageId", false);

        // When
        Intent intent = activityStarterWrapper.createContentIntent(givenMessage, notificationSettings);

        //Then
        assertNotNull(intent);
        assertEquals(notificationSettings.getIntentFlags() | Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
        Message message = Message.createFrom(intent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE));
        assertJEquals(givenMessage, message);
        assertEquals(intent.getAction(), intent.getAction());
    }

}
