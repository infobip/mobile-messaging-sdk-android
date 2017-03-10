package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.infobip.mobile.messaging.tools.InfobipAndroidTestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Date;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author sslavin
 * @since 10/11/2016.
 */

public class UserDataSyncTest extends InfobipAndroidTestCase {

    BroadcastReceiver receiver;
    ArgumentCaptor<Intent> captor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        captor = ArgumentCaptor.forClass(Intent.class);
        receiver = Mockito.mock(BroadcastReceiver.class);
        context.registerReceiver(receiver, new IntentFilter(Event.USER_DATA_REPORTED.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        context.unregisterReceiver(receiver);

        super.tearDown();
    }

    public void test_empty_user_data() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        mobileMessaging.fetchUserData();

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        UserData userData = UserData.createFrom(captor.getValue().getExtras());

        assertTrue(userData.getPredefinedUserData() == null || userData.getPredefinedUserData().isEmpty());
        assertTrue(userData.getCustomUserData() == null || userData.getCustomUserData().isEmpty());
    }

    public void test_remove_custom_element() throws Exception {

        UserData userData = new UserData();
        userData.setCustomUserDataElement("myKey1", new CustomUserDataValue("Some string"));
        userData.setCustomUserDataElement("myKey2", new CustomUserDataValue(12345));
        userData.setCustomUserDataElement("myKey2", new CustomUserDataValue(new Date()));

        userData.removeCustomUserDataElement("myKey1");
        userData.removeCustomUserDataElement("myKey2");
        userData.removeCustomUserDataElement("myKey3");

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        mobileMessaging.syncUserData(userData);

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));

        JSONAssert.assertEquals(
                "{"  +
                        "\"customUserData\": {" +
                            "\"myKey1\":null," +
                            "\"myKey2\":null," +
                            "\"myKey3\":null"  +
                        "}" +
                "}", debugServer.getBody(), false);
    }
}
