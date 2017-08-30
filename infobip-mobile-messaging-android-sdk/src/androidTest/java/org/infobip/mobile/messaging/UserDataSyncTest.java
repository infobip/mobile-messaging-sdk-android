package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.data.UserDataReport;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 10/11/2016.
 */

public class UserDataSyncTest extends MobileMessagingTestCase {

    private ArgumentCaptor<UserDataReport> reportCaptor;
    private ArgumentCaptor<UserData> dataCaptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        reportCaptor = forClass(UserDataReport.class);
        dataCaptor = forClass(UserData.class);
        given(mobileApiData.reportUserData(anyString(), any(UserDataReport.class)))
                .willReturn(new UserDataReport());
    }

    @Test
    public void test_empty_user_data() throws Exception {

        mobileMessaging.fetchUserData();

        verify(broadcaster, after(1000).atLeastOnce()).userDataReported(dataCaptor.capture());

        UserData userData = dataCaptor.getValue();
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

        mobileMessaging.syncUserData(userData);

        verify(mobileApiData, after(1000).times(1)).reportUserData(anyString(), reportCaptor.capture());

        UserDataReport report = reportCaptor.getValue();
        assertEquals(null, report.getCustomUserData().get("myKey1"));
        assertEquals(null, report.getCustomUserData().get("myKey2"));
        assertEquals(null, report.getCustomUserData().get("myKey3"));
    }
}
