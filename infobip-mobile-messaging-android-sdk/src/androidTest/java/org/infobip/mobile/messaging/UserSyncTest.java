package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.mobile.Result;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class UserSyncTest extends MobileMessagingTestCase {

    private ArgumentCaptor<User> dataCaptor;
    private ArgumentCaptor<Result> resultCaptor;
    private MobileMessaging.ResultListener<User> resultListener;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        resultListener = mock(MobileMessaging.ResultListener.class);
        dataCaptor = forClass(User.class);
        resultCaptor = forClass(Result.class);
        given(mobileApiAppInstance.getUser(anyString())).willReturn(new UserBody());
    }

    @Test
    public void test_user_data_fetch() throws Exception {
        mobileMessaging.fetchUser(resultListener);

        verify(mobileApiAppInstance, after(500).times(1)).getUser(anyString());
        verify(resultListener, after(300).times(1)).onResult(resultCaptor.capture());
        Result result = resultCaptor.getValue();
        assertNotNull(result.getData());
        assertTrue(result.isSuccess());
        assertNull(result.getError());
    }

    @Test
    public void test_add_tags() throws Exception {
        final User givenUser = new User();
        givenUser.setTags(CollectionUtils.setOf("first", "second", "third"));

        mobileMessaging.saveUser(givenUser);

        HashMap<String, Object> report = new HashMap<>();
        report.put(UserAtts.tags, CollectionUtils.setOf("first", "second", "third"));

        verify(mobileApiAppInstance, after(500).times(1)).patchUser(anyString(), anyBoolean(), eq(report));
        verify(broadcaster, after(500).atLeastOnce()).userUpdated(dataCaptor.capture());
        assertNull(mobileMessagingCore.getUnreportedUserData());

        assertEquals(givenUser.getTags(), report.get(UserAtts.tags));
        assertJEquals(givenUser.getTags(), mobileMessagingCore.getUser().getTags());
    }

    @Test
    public void test_add_standard_atts() throws Exception {
        User givenUser = new User();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2000, 1, 1);
        givenUser.setBirthday(new Date(calendar.getTimeInMillis()));
        givenUser.setGender(User.Gender.Male);
        givenUser.setFirstName("Darth");
        givenUser.setMiddleName("Beloved");
        givenUser.setLastName("Vader");
        givenUser.setExternalUserId("father_of_luke");
        givenUser.setEmails(CollectionUtils.setOf("darth_vader@mail.com"));
        givenUser.setPhones(CollectionUtils.setOf("385991111666"));

        HashMap<String, Object> report = new HashMap<>();
        report.put(UserAtts.birthday, "2000-02-01");
        report.put(UserAtts.gender, "Male");
        report.put(UserAtts.firstName, "Darth");
        report.put(UserAtts.middleName, "Beloved");
        report.put(UserAtts.lastName, "Vader");
        report.put(UserAtts.externalUserId, "father_of_luke");
        report.put(UserAtts.emails, backendEmails("darth_vader@mail.com"));
        report.put(UserAtts.phones, backendPhoneNumbers("385991111666"));

        mobileMessaging.saveUser(givenUser);

        verify(mobileApiAppInstance, after(500).times(1)).patchUser(anyString(), anyBoolean(), eq(report));
        verify(broadcaster, after(500).atLeastOnce()).userUpdated(dataCaptor.capture());
        assertNull(mobileMessagingCore.getUnreportedUserData());
    }

    @Test
    public void test_add_custom_element() throws Exception {

        User givenUser = new User();
        givenUser.setCustomAttribute("myKey1", new CustomAttributeValue("Some string"));
        givenUser.setCustomAttribute("myKey2", new CustomAttributeValue(12345));
        givenUser.setCustomAttribute("myKey3", new CustomAttributeValue(new Date()));
        givenUser.setCustomAttribute("myKey4", new CustomAttributeValue(false));

        mobileMessaging.saveUser(givenUser);

        HashMap<String, Object> customAtts = new HashMap<>();
        customAtts.put("myKey1", "Some string");
        customAtts.put("myKey2", 12345);
        customAtts.put("myKey3", DateTimeUtil.DateToYMDString(new Date()));
        customAtts.put("myKey4", false);

        HashMap<String, Object> report = new HashMap<>();
        report.put(UserAtts.customAttributes, customAtts);

        verify(mobileApiAppInstance, after(500).times(1)).patchUser(anyString(), anyBoolean(), eq(report));
        verify(broadcaster, after(500).atLeastOnce()).userUpdated(dataCaptor.capture());
        assertNull(mobileMessagingCore.getUnreportedUserData());

        assertJEquals(givenUser.getCustomAttributes(), mobileMessagingCore.getUser().getCustomAttributes());
    }

    @Test
    public void test_remove_custom_element() throws Exception {

        User givenUser = new User();
        givenUser.setCustomAttribute("myKey1", new CustomAttributeValue("Some string"));
        givenUser.setCustomAttribute("myKey2", new CustomAttributeValue(12345));
        givenUser.setCustomAttribute("myKey3", new CustomAttributeValue(new Date()));

        givenUser.removeCustomAttribute("myKey2");
        givenUser.removeCustomAttribute("myKey3");

        mobileMessaging.saveUser(givenUser);

        HashMap<String, Object> customAtts = new HashMap<>();
        customAtts.put("myKey1", "Some string");
        customAtts.put("myKey2", null);
        customAtts.put("myKey3", null);

        HashMap<String, Object> report = new HashMap<>();
        report.put(UserAtts.customAttributes, customAtts);

        verify(mobileApiAppInstance, after(500).times(1)).patchUser(anyString(), anyBoolean(), eq(report));
        verify(broadcaster, after(500).atLeastOnce()).userUpdated(dataCaptor.capture());
        assertNull(mobileMessagingCore.getUnreportedUserData());

        assertEquals(1, mobileMessagingCore.getUser().getCustomAttributes().size());
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
            map.put(UserAtts.phoneNumber, phoneNumber);
            list.add(map);
        }
        return list;
    }
}
