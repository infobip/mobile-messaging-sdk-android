package org.infobip.mobile.messaging.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.SqliteDatabaseProvider;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author sslavin
 * @since 10/03/2017.
 */

public class InfobipAndroidTestCase extends InstrumentationTestCase {

    protected Context context;
    protected Context contextMock;
    protected DebugServer debugServer;
    protected MobileMessaging mobileMessaging;
    protected MobileMessagingCore mobileMessagingCore;
    protected DatabaseHelper databaseHelper;
    protected SqliteDatabaseProvider databaseProvider;

    @SuppressLint("ApplySharedPref")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();
        contextMock = mockContext(context);

        debugServer = new DebugServer();
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, "{\n" +
                "  \"code\": \"500\",\n" +
                "  \"message\": \"Internal server error\"\n" +
                "}");
        debugServer.start();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, "TestRegistrationId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, true);

        MobileApiResourceProvider.INSTANCE.resetMobileApi();

        mobileMessaging = MobileMessaging.getInstance(context);
        mobileMessagingCore = MobileMessagingCore.getInstance(context);

        databaseHelper = MobileMessagingCore.getDatabaseHelper(context);
        databaseProvider = MobileMessagingCore.getDatabaseProvider(context);
    }

    private static Context mockContext(final Context realContext) {
        Context contextSpy = Mockito.mock(Context.class);

        Mockito.when(contextSpy.getSharedPreferences(Mockito.anyString(), Mockito.anyInt())).thenAnswer(new Answer<SharedPreferences>() {
            @Override
            public SharedPreferences answer(InvocationOnMock invocation) throws Throwable {
                Object arguments[] = invocation.getArguments();
                return realContext.getSharedPreferences((String) arguments[0], (Integer) arguments[1]);
            }
        });
        Mockito.when(contextSpy.getPackageManager()).thenReturn(realContext.getPackageManager());
        Mockito.when(contextSpy.getApplicationInfo()).thenReturn(realContext.getApplicationInfo());
        Mockito.when(contextSpy.getApplicationContext()).thenReturn(realContext.getApplicationContext());
        Mockito.when(contextSpy.getMainLooper()).thenReturn(realContext.getMainLooper());
        Mockito.when(contextSpy.getPackageName()).thenReturn(realContext.getPackageName());
        Mockito.when(contextSpy.getResources()).thenReturn(realContext.getResources());

        return contextSpy;
    }

    @Override
    protected void tearDown() throws Exception {
        mobileMessagingCore.shutdownTasks(30, TimeUnit.SECONDS);
        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception ignored) {
            }
        }
        databaseProvider.deleteDatabase();
        super.tearDown();
    }
}
