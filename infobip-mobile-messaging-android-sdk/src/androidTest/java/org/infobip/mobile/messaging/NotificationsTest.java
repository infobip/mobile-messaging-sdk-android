package org.infobip.mobile.messaging;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashSet;
import java.util.Set;

/**
 * @author sslavin
 * @since 30/12/2016.
 */

public class NotificationsTest extends InstrumentationTestCase {

    private Context context;
    private Context contextMock;
    private MobileMessageHandler mobileMessageHandler;
    private NotificationManager notificationManager;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();
        contextMock = Mockito.spy(Context.class);
        mobileMessageHandler = new MobileMessageHandler();
        notificationManager = Mockito.mock(NotificationManager.class);

        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.NOTIFICATION_SERVICE))).thenReturn(notificationManager);
        mockContext();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
    }

    private void mockContext() {
        Mockito.when(contextMock.getSharedPreferences(Mockito.anyString(), Mockito.anyInt())).thenAnswer(new Answer<SharedPreferences>() {
            @Override
            public SharedPreferences answer(InvocationOnMock invocation) throws Throwable {
                Object arguments[] = invocation.getArguments();
                return context.getSharedPreferences((String) arguments[0], (Integer) arguments[1]);
            }
        });
        Mockito.when(contextMock.getPackageManager()).thenReturn(context.getPackageManager());
        Mockito.when(contextMock.getApplicationInfo()).thenReturn(context.getApplicationInfo());
        Mockito.when(contextMock.getApplicationContext()).thenReturn(context.getApplicationContext());
        Mockito.when(contextMock.getMainLooper()).thenReturn(context.getMainLooper());
        Mockito.when(contextMock.getPackageName()).thenReturn(context.getPackageName());
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

        Mockito.verify(notificationManager, Mockito.never()).notify(Mockito.anyInt(), Mockito.any(Notification.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void test_shouldProvideMultipleNotifications_whenMultipleNotificationsEnabled() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.MULTIPLE_NOTIFICATIONS_ENABLED, true);
        PreferenceHelper.saveClass(context, MobileMessagingProperty.CALLBACK_ACTIVITY, Activity.class);

        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.messageId", "messageId");
        bundle.putString("gcm.notification.title", "title");
        bundle.putString("gcm.notification.body", "body");
        bundle.putString("gcm.notification.sound", "default");
        bundle.putString("gcm.notification.vibrate", "true");
        bundle.putString("gcm.notification.silent", "false");

        Intent intent = new Intent();
        intent.putExtras(bundle);

        for (int i = 0; i < 10; i++) {
            mobileMessageHandler.handleMessage(contextMock, intent);
        }

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(notificationManager, Mockito.times(10)).notify(captor.capture(), Mockito.any(Notification.class));

        Set<Integer> notificationIDs = new HashSet<>(captor.getAllValues());
        assertEquals(10, notificationIDs.size());
    }
}
