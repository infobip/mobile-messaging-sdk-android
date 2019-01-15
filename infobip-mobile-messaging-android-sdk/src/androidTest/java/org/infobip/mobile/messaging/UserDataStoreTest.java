package org.infobip.mobile.messaging;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class UserDataStoreTest extends MobileMessagingTestCase {

    private ArgumentCaptor<UserData> captor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        captor = ArgumentCaptor.forClass(UserData.class);
    }

    @Test
    public void test_should_save_user_data_on_disk() throws Exception {

        // Given
        UserData givenUserData = userData();

        // When
        mobileMessagingCore.setUserDataReported(givenUserData, false);

        // Then
        UserData userData = mobileMessagingCore.getUserData();
        assertJEquals(givenUserData, userData, "map");
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_should_not_save_user_data_on_disk() throws Exception {

        // Given
        withoutStoringUserData();
        UserData givenUserData = userData();

        // When
        mobileMessagingCore.setUserDataReported(givenUserData, false);

        // Then
        UserData userData = mobileMessagingCore.getUserData();
        assertNull(userData);
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_should_sync_user_data_if_opted_out() throws Exception {

        // Given
        withoutStoringUserData();
        UserData givenUserData = userData();

        // When
        mobileMessagingCore.saveUserData(givenUserData);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userDataReported(captor.capture());
        assertNotNull(captor.getValue());
    }

    private void withoutStoringUserData() {
        PreferenceHelper.saveBoolean(contextMock, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK, false);
    }

    @NonNull
    private UserData userData() {
        return new UserData(
                "someUserId",
                "User",
                "Tester",
                null,
                null,
                "2000-01-01",
                null,
                null,
                null,
                null,
                null);
    }
}
