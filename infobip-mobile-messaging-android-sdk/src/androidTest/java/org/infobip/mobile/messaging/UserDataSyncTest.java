package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Date;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertTrue;

/**
 * @author sslavin
 * @since 10/11/2016.
 */

public class UserDataSyncTest extends MobileMessagingTestCase {

    private ArgumentCaptor<UserData> captor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        captor = ArgumentCaptor.forClass(UserData.class);
    }

    @Test
    public void test_empty_user_data() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        mobileMessaging.fetchUserData();

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userDataReported(captor.capture());

        UserData userData = captor.getValue();
        assertTrue(userData.getPredefinedUserData() == null || userData.getPredefinedUserData().isEmpty());
        assertTrue(userData.getCustomUserData() == null || userData.getCustomUserData().isEmpty());
    }

    @Test
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

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userDataReported(captor.capture());

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
