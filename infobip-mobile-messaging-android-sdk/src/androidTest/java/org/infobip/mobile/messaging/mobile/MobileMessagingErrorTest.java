package org.infobip.mobile.messaging.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.DebugServer;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;

public class MobileMessagingErrorTest extends InstrumentationTestCase {

    private ArgumentCaptor<Intent> captor;
    private BroadcastReceiver receiver;
    private DebugServer debugServer;
    private Context context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        debugServer = new DebugServer();
        debugServer.start();

        context = getInstrumentation().getContext();
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");

        captor = ArgumentCaptor.forClass(Intent.class);
        receiver = Mockito.mock(BroadcastReceiver.class);
        context.registerReceiver(receiver, new IntentFilter(Event.API_COMMUNICATION_ERROR.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        getInstrumentation().getContext().unregisterReceiver(receiver);

        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception e) {
                //ignore
            }
        }

        super.tearDown();
    }

    public void test_printMobileMessagingServerError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, "{'requestError':{'serviceException':{'messageId':'3','text':'Some weird error'}}}");

        RegistrationSynchronizer registrationSynchronizer = new RegistrationSynchronizer();
        registrationSynchronizer.synchronize(context, "deviceApplicationInstanceId", "TestDeviceInstanceId", false, new MobileMessagingStats(context), Executors.newSingleThreadExecutor());

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        Intent intent = captor.getValue();
        MobileMessagingError mobileMessagingError = (MobileMessagingError) intent.getSerializableExtra(EXTRA_EXCEPTION);

        assertTrue(intent.hasExtra(BroadcastParameter.EXTRA_EXCEPTION));
        assertNotNull(mobileMessagingError);
        assertEquals(MobileMessagingError.Type.SERVER_ERROR, mobileMessagingError.getType());
        assertEquals("3", mobileMessagingError.getCode());
        assertEquals("Some weird error", mobileMessagingError.getMessage());
    }

    public void test_unknownError() throws Exception {
        MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(new Exception("Some exception"));

        assertEquals(MobileMessagingError.Type.UNKNOWN_ERROR, mobileMessagingError.getType());
        assertEquals("-10", mobileMessagingError.getCode());
        assertEquals("Some exception", mobileMessagingError.getMessage());
    }
}
