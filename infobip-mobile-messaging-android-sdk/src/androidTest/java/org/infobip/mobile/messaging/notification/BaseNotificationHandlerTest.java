package org.infobip.mobile.messaging.notification;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertEquals;

/**
 * @author sslavin
 * @since 30/12/2016.
 */

public class BaseNotificationHandlerTest extends MobileMessagingTestCase {

    private NotificationManager notificationManagerMock;
    private ArgumentCaptor<Integer> notificationCaptor;
    private BaseNotificationHandler simpleNotificationHandler;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        simpleNotificationHandler = new BaseNotificationHandler(contextMock);
        notificationCaptor = ArgumentCaptor.forClass(Integer.class);
        notificationManagerMock = Mockito.mock(NotificationManager.class);

        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.NOTIFICATION_SERVICE))).thenReturn(notificationManagerMock);
    }

    @Test
    public void shouldNotProduceNPE_whenMessageArrivesAndNotificationsDisabled() {
        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, false);
        Message givenMessage = createMessage(context, "SomeMessageId", false);

        // When
        NotificationCompat.Builder builder = simpleNotificationHandler.createNotificationCompatBuilder(givenMessage);
        int notificationId = simpleNotificationHandler.getNotificationId(givenMessage);
        simpleNotificationHandler.displayNotification(builder, givenMessage, notificationId);

        // Then
        Mockito.verify(notificationManagerMock, Mockito.never()).notify(Mockito.anyInt(), Mockito.any(Notification.class));
    }

    @Test
    public void shouldProvideMultipleNotifications_whenMultipleNotificationsEnabled() {
        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.MULTIPLE_NOTIFICATIONS_ENABLED, true);
        PreferenceHelper.saveClass(context, MobileMessagingProperty.CALLBACK_ACTIVITY, Activity.class);

        // When
        for (int i = 0; i < 10; i++) {
            Message message = new Message();
            message.setBody("SomeText");

            NotificationCompat.Builder builder = simpleNotificationHandler.createNotificationCompatBuilder(message);
            int notificationId = simpleNotificationHandler.getNotificationId(message);
            simpleNotificationHandler.displayNotification(builder, message, notificationId);
        }

        // Then
        Mockito.verify(notificationManagerMock, Mockito.times(10)).notify(notificationCaptor.capture(), Mockito.any(Notification.class));
        Set<Integer> notificationIDs = new HashSet<>(notificationCaptor.getAllValues());
        assertEquals(10, notificationIDs.size());
    }

    @Test
    public void shouldTryToFetchMediaMultipleTimes_ifContentUrlSupplied_andThereAreNetworkFailures() {
        // Given
        String givenInvalidPictureUrl = "http://127.0.0.1:" + debugServer.getListeningPort() + "/";
        int givenMaxRetryCount = 10;
        PreferenceHelper.saveInt(context, MobileMessagingProperty.DEFAULT_MAX_RETRY_COUNT, givenMaxRetryCount);
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);

        // When
        simpleNotificationHandler.fetchNotificationPicture(givenInvalidPictureUrl);

        // Then
        assertEquals(givenMaxRetryCount, debugServer.getRequestCount());
    }

    @Test
    public void shouldFetchMediaSuccessfullyOnce() {
        // Given
        String givenPictureUrl = prepareBitmapUrl();

        // When
        simpleNotificationHandler.fetchNotificationPicture(givenPictureUrl);

        // Then
        assertEquals(1, debugServer.getRequestCount());
    }

    @Test
    public void shouldSetHightPriorityForMessageWithBanner() {

        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, true);
        PreferenceHelper.saveClass(context, MobileMessagingProperty.CALLBACK_ACTIVITY, Activity.class);

        // When
        NotificationCompat.Builder builder = simpleNotificationHandler.createNotificationCompatBuilder(prepareMessageWithBanner());

        // Then
        assertEquals(NotificationCompat.PRIORITY_HIGH, builder.getPriority());
    }

    @Test
    public void shouldUseHighPriorityChannelForMessageWithBanner() throws Exception {

        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, true);
        PreferenceHelper.saveClass(context, MobileMessagingProperty.CALLBACK_ACTIVITY, Activity.class);

        // When
        NotificationCompat.Builder builder = simpleNotificationHandler.createNotificationCompatBuilder(prepareMessageWithBanner());

        // Then
        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID, getChannelId(builder));
    }

    private String prepareBitmapUrl() {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_media_play);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "image/png", new ByteArrayInputStream(bitmapdata));
        return "http://127.0.0.1:" + debugServer.getListeningPort() + "/";
    }

    private Message prepareMessageWithBanner() {
        return new Message(
                UUID.randomUUID().toString(),
                "SomeTitle",
                "SomeBody",
                "SomeSound",
                true,
                "SomeIcon",
                false,
                "SomeCategory",
                "SomeFrom",
                0,
                0,
                0,
                null,
                null,
                "SomeDestination",
                Message.Status.SUCCESS,
                "SomeStatusMessage",
                "http://www.some-content.com.ru.hr",
                Message.InAppStyle.BANNER,
                0,
                null,
                null,
                null,
                null
        );
    }

    // getting via reflection due to API issues on different android versions
    private static String getChannelId(NotificationCompat.Builder builder) throws Exception {
        Field field = NotificationCompat.Builder.class.getDeclaredField("mChannelId");
        field.setAccessible(true);
        return (String) field.get(builder);
    }
}
