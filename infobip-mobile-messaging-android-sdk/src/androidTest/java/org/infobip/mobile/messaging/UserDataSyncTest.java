package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Calendar;
import java.util.Date;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class UserDataSyncTest extends MobileMessagingTestCase {

    private ArgumentCaptor<UserBody> reportCaptor;
    private ArgumentCaptor<UserData> dataCaptor;
    private MobileMessaging.ResultListener resultListener;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        resultListener = mock(MobileMessaging.ResultListener.class);
        reportCaptor = forClass(UserBody.class);
        dataCaptor = forClass(UserData.class);
        given(mobileApiAppInstance.getUser(anyString())).willReturn(new UserBody());
    }

    @Test
    public void test_empty_user_data() throws Exception {
        mobileMessaging.fetchUserData(resultListener);

        verify(broadcaster, after(1000).atLeastOnce()).userDataAcquired(dataCaptor.capture());

        UserData userData = dataCaptor.getValue();
        assertTrue(userData.getStandardAttributes() == null || userData.getStandardAttributes().isEmpty());
        assertTrue(userData.getCustomAttributes() == null || userData.getCustomAttributes().isEmpty());
    }

    @Test
    public void test_add_birthday() throws Exception {
        UserData userData = new UserData();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, 1, 1);
        userData.setBirthday(new Date(calendar.getTimeInMillis()));

        mobileMessaging.saveUserData(userData);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), reportCaptor.capture());

        UserBody report = reportCaptor.getValue();
        assertEquals(userData.getBirthday(), DateTimeUtil.DateFromYMDString(report.getBirthday()));
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

        mobileMessaging.saveUserData(userData);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), reportCaptor.capture());

        UserBody report = reportCaptor.getValue();
        assertEquals(null, report.getCustomAttributes().get("myKey1"));
        assertEquals(null, report.getCustomAttributes().get("myKey2"));
        assertEquals(null, report.getCustomAttributes().get("myKey3"));
    }
}
