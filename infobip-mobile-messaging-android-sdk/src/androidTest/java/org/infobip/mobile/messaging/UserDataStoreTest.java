package org.infobip.mobile.messaging;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Calendar;
import java.util.Date;

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
        mobileMessagingCore.setUserDataReported(givenUserData);

        // Then
        UserData userData = mobileMessagingCore.getUserData();
        assertJEquals(givenUserData, userData);
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_should_not_save_user_data_on_disk() throws Exception {

        // Given
        withoutStoringUserData();
        UserData givenUserData = userData();

        // When
        mobileMessagingCore.setUserDataReported(givenUserData);

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
        UserData userData = new UserData();
        userData.setExternalUserId("someUserId");
        userData.setFirstName("User");
        userData.setLastName("Tester");
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, 1, 1);
        userData.setBirthday(new Date(calendar.getTimeInMillis()));
        return userData;
    }
}
