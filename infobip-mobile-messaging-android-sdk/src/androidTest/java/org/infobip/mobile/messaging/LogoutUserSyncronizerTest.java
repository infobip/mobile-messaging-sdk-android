package org.infobip.mobile.messaging;


import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;

public class LogoutUserSyncronizerTest extends MobileMessagingTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        given(mobileApiData.logoutUser()).willReturn(null);

        enableMessageStoreForReceivedMessages();
    }

    @Test
    public void test_empty_user_data_and_storage_after_logout() throws Exception {

        //given
        UserData userData = new UserData();
        userData.setFirstName("John");
        userData.setCustomUserDataElement("someKey", new CustomUserDataValue("someValue"));
        SystemData systemData = new SystemData("SomeSdkVersion", "SomeOsVersion", "SomeDeviceManufacturer", "SomeDeviceModel", "SomeAppVersion", false, true, true);
        PreferenceHelper.saveString(context, MobileMessagingProperty.USER_DATA, userData.toString());
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, systemData.toString());
        createMessage(context, "SomeMessageId", true);

        assertEquals(1, MobileMessaging.getInstance(context).getMessageStore().findAll(context).size());

        //when
        mobileMessaging.logoutUser();

        //then
        verify(broadcaster, after(1000).atLeastOnce()).userLoggedOut();

        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.USER_DATA));
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_USER_DATA));
        assertFalse(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA).isEmpty());
        assertEquals(0, MobileMessaging.getInstance(context).getMessageStore().findAll(context).size());
    }
}
