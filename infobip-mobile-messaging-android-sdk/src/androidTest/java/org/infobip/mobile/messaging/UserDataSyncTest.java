package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public void test_add_tags() throws Exception {
        UserData givenUserData = new UserData();
        HashSet<String> tags = new HashSet<>();
        tags.add("first");
        tags.add("second");
        tags.add("third");
        givenUserData.setTags(tags);

        mobileMessaging.saveUserData(givenUserData);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), reportCaptor.capture());

        UserBody report = reportCaptor.getValue();
        assertEquals(givenUserData.getTags(), report.getTags());

        assertJEquals(givenUserData.getTags(), mobileMessagingCore.getUserData().getTags());
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_add_standard_atts() throws Exception {
        UserData givenUserData = new UserData();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, 1, 1);
        givenUserData.setBirthday(new Date(calendar.getTimeInMillis()));
        givenUserData.setGender(UserData.Gender.Male);
        givenUserData.setFirstName("Darth");
        givenUserData.setMiddleName("Beloved");
        givenUserData.setLastName("Vader");
        givenUserData.setExternalUserId("father_of_luke");
        givenUserData.setEmails(Collections.singletonList("darth_vader@mail.com"));
        givenUserData.setGsms(Collections.singletonList("385991111666"));

        mobileMessaging.saveUserData(givenUserData);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), reportCaptor.capture());

        UserBody report = reportCaptor.getValue();
        assertEquals(givenUserData.getBirthday(), DateTimeUtil.DateFromYMDString(report.getBirthday()));
        assertEquals(givenUserData.getGender(), UserData.Gender.valueOf(report.getGender()));
        assertEquals(givenUserData.getFirstName(), report.getFirstName());
        assertEquals(givenUserData.getMiddleName(), report.getMiddleName());
        assertEquals(givenUserData.getLastName(), report.getLastName());
        assertEquals(givenUserData.getExternalUserId(), report.getExternalUserId());
        assertEquals(givenUserData.getEmailsWithPreferred(), report.getEmails());
        assertEquals(givenUserData.getGsmsWithPreferred(), report.getGsms());
        assertJEquals(givenUserData.getStandardAttributes(), mobileMessagingCore.getUserData().getStandardAttributes());

        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_add_custom_element() throws Exception {

        UserData givenUserData = new UserData();
        givenUserData.setCustomUserDataElement("myKey1", new CustomUserDataValue("Some string"));
        givenUserData.setCustomUserDataElement("myKey2", new CustomUserDataValue(12345));
        givenUserData.setCustomUserDataElement("myKey3", new CustomUserDataValue(new Date()));

        mobileMessaging.saveUserData(givenUserData);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), reportCaptor.capture());

        UserBody report = reportCaptor.getValue();
        assertEquals(givenUserData.getCustomUserDataValue("myKey1").stringValue(), report.getCustomAttributes().get("myKey1"));
        assertEquals(givenUserData.getCustomUserDataValue("myKey2").numberValue(), report.getCustomAttributes().get("myKey2"));
        assertEquals(givenUserData.getCustomUserDataValue("myKey3").dateValue(), DateTimeUtil.DateFromYMDString((String) report.getCustomAttributes().get("myKey3")));

        assertJEquals(givenUserData.getCustomAttributes(), mobileMessagingCore.getUserData().getCustomAttributes());
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_remove_custom_element() throws Exception {

        UserData givenUserData = new UserData();
        givenUserData.setCustomUserDataElement("myKey1", new CustomUserDataValue("Some string"));
        givenUserData.setCustomUserDataElement("myKey2", new CustomUserDataValue(12345));
        givenUserData.setCustomUserDataElement("myKey3", new CustomUserDataValue(new Date()));

        givenUserData.removeCustomUserDataElement("myKey2");
        givenUserData.removeCustomUserDataElement("myKey3");

        mobileMessaging.saveUserData(givenUserData);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), reportCaptor.capture());

        UserBody report = reportCaptor.getValue();
        assertEquals("Some string", report.getCustomAttributes().get("myKey1"));
        assertEquals(null, report.getCustomAttributes().get("myKey2"));
        assertEquals(null, report.getCustomAttributes().get("myKey3"));

        assertEquals(1, mobileMessagingCore.getUserData().getCustomAttributes().size());
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }
}
