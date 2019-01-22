package org.infobip.mobile.messaging;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class UserStoreTest extends MobileMessagingTestCase {

    private ArgumentCaptor<User> captor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        captor = ArgumentCaptor.forClass(User.class);
    }

    @Test
    public void test_should_save_user_data_on_disk() throws Exception {

        // Given
        User givenUser = userData();

        // When
        mobileMessagingCore.setUserDataReported(givenUser, false);

        // Then
        User user = mobileMessagingCore.getUser();
        assertJEquals(givenUser, user, "map");
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_should_not_save_user_data_on_disk() throws Exception {

        // Given
        withoutStoringUserData();
        User givenUser = userData();

        // When
        mobileMessagingCore.setUserDataReported(givenUser, false);

        // Then
        User user = mobileMessagingCore.getUser();
        assertNull(user);
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_should_sync_user_data_if_opted_out() throws Exception {

        // Given
        withoutStoringUserData();
        User givenUser = userData();

        // When
        mobileMessagingCore.saveUser(givenUser);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userUpdated(captor.capture());
        assertNotNull(captor.getValue());
    }

    private void withoutStoringUserData() {
        PreferenceHelper.saveBoolean(contextMock, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK, false);
    }

    @NonNull
    private User userData() {
        return new User(
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
