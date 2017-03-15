package org.infobip.mobile.messaging;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;

import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

/**
 * @author sslavin
 * @since 30/12/2016.
 */

public class NotificationsTest extends MobileMessagingTestCase {

    private MobileMessageHandler mobileMessageHandler;
    private NotificationManager notificationManagerMock;
    private ArgumentCaptor<Integer> notificationCaptor;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mobileMessageHandler = new MobileMessageHandler(new AndroidBroadcaster(context));
        notificationCaptor = ArgumentCaptor.forClass(Integer.class);
        notificationManagerMock = Mockito.mock(NotificationManager.class);

        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.NOTIFICATION_SERVICE))).thenReturn(notificationManagerMock);
    }

    public void test_shouldNotProduceNPE_whenMessageArrivesAndNotificationsDisabled() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, false);

        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.messageId", "messageId");
        bundle.putString("gcm.notification.title", "title");
        bundle.putString("gcm.notification.body", "body");
        bundle.putString("gcm.notification.sound", "default");
        bundle.putString("gcm.notification.vibrate", "true");
        bundle.putString("gcm.notification.silent", "false");

        Intent intent = new Intent();
        intent.putExtras(bundle);

        // should not produce NPE
        mobileMessageHandler.handleMessage(contextMock, intent);

        Mockito.verify(notificationManagerMock, Mockito.never()).notify(Mockito.anyInt(), Mockito.any(Notification.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void test_shouldProvideMultipleNotifications_whenMultipleNotificationsEnabled() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.MULTIPLE_NOTIFICATIONS_ENABLED, true);
        PreferenceHelper.saveClass(context, MobileMessagingProperty.CALLBACK_ACTIVITY, Activity.class);

        for (int i = 0; i < 10; i++) {
            Message message = new Message();
            message.setBody("SomeText");
            mobileMessageHandler.handleMessage(contextMock, message);
        }

        Mockito.verify(notificationManagerMock, Mockito.times(10)).notify(notificationCaptor.capture(), Mockito.any(Notification.class));

        Set<Integer> notificationIDs = new HashSet<>(notificationCaptor.getAllValues());
        assertEquals(10, notificationIDs.size());
    }
}
