package org.infobip.mobile.messaging.cloud;

import static org.mockito.Mockito.doReturn;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.cloud.firebase.FirebaseAppProvider;
import org.infobip.mobile.messaging.platform.PlatformTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 05/09/2018.
 */
@RunWith(AndroidJUnit4.class)
public class MobileMessagingCloudServiceTest extends PlatformTestCase {

    private final MobileMessagingCloudHandler handler = Mockito.mock(MobileMessagingCloudHandler.class);
    private final Context context = Mockito.mock(Context.class);
    private FirebaseAppProvider firebaseAppProviderSpy;
    private final FirebaseApp firebaseApp = Mockito.mock(FirebaseApp.class);

    @Before
    public void beforeEach() {
        Mockito.reset(handler);
        resetMobileMessagingCloudHandler(handler);
        resetBackgroundExecutor(Runnable::run);

        // will verify only below "O" logic since after that it will go deep into JobIntentService (cannot mock)
        resetSdkVersion(Build.VERSION_CODES.N_MR1);
        Mockito.when(context.checkPermission(Mockito.eq(Manifest.permission.WAKE_LOCK), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(PackageManager.PERMISSION_DENIED);


        FirebaseAppProvider firebaseAppProvider = new FirebaseAppProvider(context);
        firebaseAppProviderSpy = Mockito.spy(firebaseAppProvider);

        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder().setProjectId("project_id").setApiKey("api_key").setApplicationId("application_id").build();
        firebaseAppProviderSpy.setFirebaseOptions(firebaseOptions);
        doReturn(firebaseApp).when(firebaseAppProviderSpy).getFirebaseApp();
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
        MobileMessagingCloudService.enqueueNewToken(context, "token");
        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), intentWith("token"));
    }

    @Test
    public void test_shouldHandleTokenCleanup() {
        MobileMessagingCloudService.enqueueTokenCleanup(context, firebaseAppProviderSpy);
        Mockito.verify(firebaseAppProviderSpy, Mockito.times(1)).getFirebaseApp();
        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    @Test
    public void test_shouldHandleTokenReset() {
        MobileMessagingCloudService.enqueueTokenReset(context, firebaseAppProviderSpy);
        Mockito.verify(firebaseAppProviderSpy, Mockito.times(1)).getFirebaseApp();
        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    @Test
    public void test_shouldHandleTokenAcquisition() {
        MobileMessagingCloudService.enqueueTokenAcquisition(context, firebaseAppProviderSpy);
        Mockito.verify(firebaseAppProviderSpy, Mockito.times(1)).getFirebaseApp();
        Mockito.verify(handler, Mockito.times(1)).handleWork(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    private static Intent intentWith(final Message message) {
        return Mockito.argThat(o -> {
            Message that = Message.createFrom(o.getExtras());
            return message.getBody().equals(that.getBody())
                    && message.getMessageId().equals(that.getMessageId());
        });
    }

    @SuppressWarnings("SameParameterValue")
    private static Intent intentWith(final String token) {
        return Mockito.argThat(o -> {
            return token.equals(o.getStringExtra(MobileMessagingCloudHandler.EXTRA_TOKEN));
        });
    }
}
