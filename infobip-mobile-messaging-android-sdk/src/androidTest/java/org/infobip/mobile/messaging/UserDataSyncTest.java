package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class UserDataSyncTest extends MobileMessagingTestCase {

    private ArgumentCaptor<UserData> dataCaptor;
    private MobileMessaging.ResultListener resultListener;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        resultListener = mock(MobileMessaging.ResultListener.class);
        dataCaptor = forClass(UserData.class);
        given(mobileApiAppInstance.getUser(anyString())).willReturn(new UserBody());
    }

    @Test
    public void test_empty_user_data() throws Exception {
        mobileMessaging.fetchUser(resultListener);

        verify(broadcaster, after(1000).atLeastOnce()).userDataAcquired(dataCaptor.capture());

        UserData userData = dataCaptor.getValue();
        assertTrue(userData.getCustomAttributes() == null || userData.getCustomAttributes().isEmpty());
    }

    @Test
    public void test_add_tags() throws Exception {
        final UserData givenUserData = new UserData();
        givenUserData.setTags(CollectionUtils.setOf("first", "second", "third"));

        mobileMessaging.saveUser(givenUserData);

        HashMap<String, Object> report = new HashMap<>();
        report.put(UserAtts.tags, CollectionUtils.setOf("first", "second", "third"));

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), eq(report));

        assertEquals(givenUserData.getTags(), report.get(UserAtts.tags));
        assertJEquals(givenUserData.getTags(), mobileMessagingCore.getUser().getTags());
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
        givenUserData.setEmails(CollectionUtils.setOf("darth_vader@mail.com"));
        givenUserData.setGsms(CollectionUtils.setOf("385991111666"));

        HashMap<String, Object> report = new HashMap<>();
        report.put(UserAtts.birthday, "2000-02-01");
        report.put(UserAtts.gender, "Male");
        report.put(UserAtts.firstName, "Darth");
        report.put(UserAtts.middleName, "Beloved");
        report.put(UserAtts.lastName, "Vader");
        report.put(UserAtts.externalUserId, "father_of_luke");
        report.put(UserAtts.emails, backendEmails("darth_vader@mail.com"));
        report.put(UserAtts.gsms, backendPhoneNumbers("385991111666"));

        mobileMessaging.saveUser(givenUserData);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), eq(report));
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_add_custom_element() throws Exception {

        UserData givenUserData = new UserData();
        givenUserData.setCustomUserDataElement("myKey1", new CustomUserDataValue("Some string"));
        givenUserData.setCustomUserDataElement("myKey2", new CustomUserDataValue(12345));
        givenUserData.setCustomUserDataElement("myKey3", new CustomUserDataValue(new Date()));
        givenUserData.setCustomUserDataElement("myKey4", new CustomUserDataValue(false));

        mobileMessaging.saveUser(givenUserData);

        HashMap<String, Object> customAtts = new HashMap<>();
        customAtts.put("myKey1", "Some string");
        customAtts.put("myKey2", 12345);
        customAtts.put("myKey3", DateTimeUtil.DateToYMDString(new Date()));
        customAtts.put("myKey4", false);

        HashMap<String, Object> report = new HashMap<>();
        report.put(UserAtts.customAttributes, customAtts);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), eq(report));
        assertJEquals(givenUserData.getCustomAttributes(), mobileMessagingCore.getUser().getCustomAttributes());
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

        mobileMessaging.saveUser(givenUserData);

        HashMap<String, Object> customAtts = new HashMap<>();
        customAtts.put("myKey1", "Some string");
        customAtts.put("myKey2", null);
        customAtts.put("myKey3", null);

        HashMap<String, Object> report = new HashMap<>();
        report.put(UserAtts.customAttributes, customAtts);

        verify(mobileApiAppInstance, after(1000).times(1)).patchUser(anyString(), anyBoolean(), eq(report));

        assertEquals(1, mobileMessagingCore.getUser().getCustomAttributes().size());
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    private List<Map<String, Object>> backendEmails(String... emails) {
        List<Map<String, Object>> list = new ArrayList<>(emails.length);
        for (String email : emails) {
            Map<String, Object> map = new HashMap<>();
            map.put(UserAtts.emailAddress, email);
            list.add(map);
        }
        return list;
    }

    private List<Map<String, Object>> backendPhoneNumbers(String... phoneNumbers) {
        List<Map<String, Object>> list = new ArrayList<>(phoneNumbers.length);
        for (String phoneNumber : phoneNumbers) {
            Map<String, Object> map = new HashMap<>();
            map.put(UserAtts.gsmNumber, phoneNumber);
            list.add(map);
        }
        return list;
    }
}
