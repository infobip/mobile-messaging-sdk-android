package org.infobip.mobile.messaging.notification;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertEquals;

/**
 * @author sslavin
 * @since 30/12/2016.
 */

public class NotificationHandlerImplTest extends MobileMessagingTestCase {

    private NotificationManager notificationManagerMock;
    private ArgumentCaptor<Integer> notificationCaptor;
    private NotificationHandlerImpl notificationHandlerImpl;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        notificationHandlerImpl = new NotificationHandlerImpl(contextMock);
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
        notificationHandlerImpl.displayNotification(givenMessage);

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
            notificationHandlerImpl.displayNotification(message);
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
        notificationHandlerImpl.fetchNotificationPicture(givenInvalidPictureUrl);

        // Then
        assertEquals(givenMaxRetryCount, debugServer.getRequestCount());
    }

    @Test
    public void shouldFetchMediaSuccessfullyOnce() {
        // Given
        String givenPictureUrl = prepareBitmapUrl();

        // When
        notificationHandlerImpl.fetchNotificationPicture(givenPictureUrl);

        // Then
        assertEquals(1, debugServer.getRequestCount());
    }

    private String prepareBitmapUrl() {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_media_play);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "image/png", new ByteArrayInputStream(bitmapdata));
        return "http://127.0.0.1:" + debugServer.getListeningPort() + "/";
    }
}
