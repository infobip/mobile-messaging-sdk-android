package org.infobip.mobile.messaging.cloud;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.platform.PlatformTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 05/09/2018.
 */
@RunWith(AndroidJUnit4.class)
public class MobileMessagingCloudServiceTest extends PlatformTestCase {

    private MobileMessagingCloudHandler handler = Mockito.mock(MobileMessagingCloudHandler.class);
    private Context context = Mockito.mock(Context.class);

    @Before
    public void beforeEach() {
        Mockito.reset(handler);
        resetMobileMessagingCloudHandler(handler);
        resetBackgroundExecutor(new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
                command.run();
            }
        });

        // will verify only below "O" logic since after that it will go deep into JobIntentService (cannot mock)
        resetSdkVersion(Build.VERSION_CODES.N_MR1);
        Mockito.when(context.checkPermission(Mockito.eq(Manifest.permission.WAKE_LOCK), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED);
    }

    @Test
    public void test_shouldHandleMessage() {

        Message message = new Message();
        message.setBody("body");
        message.setMessageId("messageId");

        MobileMessagingCloudService.enqueueNewMessage(context, message);

        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), intentWith(message));
    }

    @Test
    public void test_shouldHandleNewToken() {
        MobileMessagingCloudService.enqueueNewToken(context, "senderId", "token");
        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), intentWith("senderId", "token"));
    }

    @Test
    public void test_shouldHandleTokenCleanup() {
        MobileMessagingCloudService.enqueueTokenCleanup(context, "senderId");
        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), intentWith("senderId"));
    }

    @Test
    public void test_shouldHandleTokenReset() {
        MobileMessagingCloudService.enqueueTokenReset(context, "senderId");
        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), intentWith("senderId"));
    }

    @Test
    public void test_shouldHandleTokenAcquisition() {
        MobileMessagingCloudService.enqueueTokenAcquisition(context, "senderId");
        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), intentWith("senderId"));
    }

    private static Intent intentWith(final Message message) {
        return Mockito.argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;

                Message that = Message.createFrom(intent.getExtras());
                return message.getBody().equals(that.getBody())
                        && message.getMessageId().equals(that.getMessageId());
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private static Intent intentWith(final String senderId) {
        return Mockito.argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return senderId.equals(intent.getStringExtra(MobileMessagingCloudHandler.EXTRA_SENDER_ID));
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private static Intent intentWith(final String senderId, final String token) {
        return Mockito.argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Object o) {
                Intent intent = (Intent) o;
                return senderId.equals(intent.getStringExtra(MobileMessagingCloudHandler.EXTRA_SENDER_ID))
                        && token.equals(intent.getStringExtra(MobileMessagingCloudHandler.EXTRA_TOKEN));
            }
        });
    }
}
