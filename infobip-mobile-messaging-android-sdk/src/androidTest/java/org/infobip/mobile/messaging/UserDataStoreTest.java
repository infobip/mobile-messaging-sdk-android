package org.infobip.mobile.messaging;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class UserDataStoreTest extends MobileMessagingTestCase {

    private MobileMessagingCore core;
    private ArgumentCaptor<UserData> captor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        core = new MobileMessagingCore(contextMock);
        captor = ArgumentCaptor.forClass(UserData.class);

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");
    }

    @Test
    public void test_should_save_user_data_on_disk() throws Exception {

        // Given
        UserData givenUserData = userData();

        // When
        core.setUserDataReported(givenUserData);

        // Then
        UserData userData = core.getUserData();
        assertJEquals(givenUserData, userData);
        assertNull(core.getUnreportedUserData());
    }

    @Test
    public void test_should_not_save_user_data_on_disk() throws Exception {

        // Given
        withoutStoringUserData();
        UserData givenUserData = userData();

        // When
        core.setUserDataReported(givenUserData);

        // Then
        UserData userData = core.getUserData();
        assertNull(userData);
        assertNull(core.getUnreportedUserData());
    }

    @Test
    public void test_should_sync_user_data_if_opted_out() throws Exception {

        // Given
        withoutStoringUserData();
        UserData givenUserData = userData();

        // When
        mobileMessagingCore.syncUserData(givenUserData);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userDataReported(captor.capture());
        assertNotNull(captor.getValue());
    }

    private void withoutStoringUserData() {
        PreferenceHelper.saveBoolean(contextMock, MobileMessagingProperty.SAVE_USER_DATA_ON_DEVICE, false);
    }

    @NonNull
    private UserData userData() {
        UserData userData = new UserData();
        userData.setEmail("email@UserDataStoreTest.com");
        userData.setExternalUserId("someUserId");
        userData.setFirstName("User");
        userData.setLastName("Tester");
        return userData;
    }
}
