package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class CustomAttributeTypeTest extends MobileMessagingTestCase {

    private ArgumentCaptor<User> userCaptor;
    private ArgumentCaptor<Installation> installationCaptor;

    private static final String KEY_FOR_STRING = "keyForString";
    private static final String KEY_FOR_EMPTY_STRING = "keyForEmptyString";
    private static final String KEY_FOR_NUMBER = "keyForNumber";
    private static final String KEY_FOR_DATE = "keyForDate";
    private static final String KEY_FOR_DATETIME = "keyForDateTime";

    private final String SOME_STRING_VALUE = "bla";
    private final String EMPTY_STRING_VALUE = "";
    private final int SOME_NUMBER_VALUE = 1111;
    private final Boolean SOME_BOOLEAN_VALUE = true;
    private final Date SOME_DATE_VALUE = new Date();
    private CustomAttributeValue.DateTime SOME_DATETIME_VALUE;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        userCaptor = ArgumentCaptor.forClass(User.class);
        installationCaptor = ArgumentCaptor.forClass(Installation.class);
        Calendar instance = Calendar.getInstance();
        instance.set(2020, 0, 1, 1, 1);
        SOME_DATETIME_VALUE = new CustomAttributeValue.DateTime(instance.getTime());
    }

    @Test
    public void test_save_user_custom_attributes() {
        User user = new User();
        user.setCustomAttribute(KEY_FOR_STRING, new CustomAttributeValue(SOME_STRING_VALUE));
        user.setCustomAttribute(KEY_FOR_NUMBER, new CustomAttributeValue(SOME_NUMBER_VALUE));
        user.setCustomAttribute(KEY_FOR_DATE, new CustomAttributeValue(SOME_DATE_VALUE));
        user.setCustomAttribute(KEY_FOR_DATETIME, new CustomAttributeValue(SOME_DATETIME_VALUE));

        setListCustomAttributes(user);
        mobileMessaging.saveUser(user);

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userUpdated(userCaptor.capture());
        User userResponse = userCaptor.getValue();
        assertEquals(SOME_STRING_VALUE, userResponse.getCustomAttributeValue(KEY_FOR_STRING).stringValue());
        assertEquals(SOME_NUMBER_VALUE, userResponse.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue().intValue());
        List<ListCustomAttributeItem> items = userResponse.getListCustomAttributeItems(KEY_FOR_LIST);
        for (ListCustomAttributeItem item: items) {
            assertEquals(SOME_NUMBER_VALUE, item.getNumberValue(KEY_FOR_LIST_PARAM_1).intValue());
            assertEquals(SOME_BOOLEAN_VALUE, item.getBooleanValue(KEY_FOR_LIST_PARAM_2));
            assertEquals(SOME_STRING_VALUE, item.getStringValue(KEY_FOR_LIST_PARAM_3));
            assertEquals(DateTimeUtil.dateToYMDString(SOME_DATE_VALUE), DateTimeUtil.dateToYMDString(item.getDateValue(KEY_FOR_LIST_PARAM_4)));
        }
        assertEquals(DateTimeUtil.dateToYMDString(SOME_DATE_VALUE), DateTimeUtil.dateToYMDString(userResponse.getCustomAttributeValue(KEY_FOR_DATE).dateValue()));
        assertEquals(DateTimeUtil.dateTimeToISO8601UTCString(SOME_DATETIME_VALUE), DateTimeUtil.dateTimeToISO8601UTCString(userResponse.getCustomAttributeValue(KEY_FOR_DATETIME).dateTimeValue()));
        assertEquals(CustomAttributeValue.Type.String, userResponse.getCustomAttributeValue(KEY_FOR_STRING).getType());
        assertEquals(CustomAttributeValue.Type.Number, userResponse.getCustomAttributeValue(KEY_FOR_NUMBER).getType());
        assertEquals(CustomAttributeValue.Type.Date, userResponse.getCustomAttributeValue(KEY_FOR_DATE).getType());
        assertEquals(CustomAttributeValue.Type.DateTime, userResponse.getCustomAttributeValue(KEY_FOR_DATETIME).getType());
    }

    @Test
    public void test_save_installation_custom_attributes() {
        Installation installation = new Installation();
        installation.setCustomAttribute(KEY_FOR_STRING, new CustomAttributeValue(SOME_STRING_VALUE));
        installation.setCustomAttribute(KEY_FOR_NUMBER, new CustomAttributeValue(SOME_NUMBER_VALUE));
        installation.setCustomAttribute(KEY_FOR_DATE, new CustomAttributeValue(SOME_DATE_VALUE));
        installation.setCustomAttribute(KEY_FOR_DATETIME, new CustomAttributeValue(SOME_DATETIME_VALUE));
        mobileMessaging.saveInstallation(installation);

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).installationUpdated(installationCaptor.capture());
        Installation installationResponse = installationCaptor.getValue();
        assertEquals(SOME_STRING_VALUE, installationResponse.getCustomAttributeValue(KEY_FOR_STRING).stringValue());
        assertEquals(SOME_NUMBER_VALUE, installationResponse.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue().intValue());
        assertEquals(DateTimeUtil.dateToYMDString(SOME_DATE_VALUE), DateTimeUtil.dateToYMDString(installationResponse.getCustomAttributeValue(KEY_FOR_DATE).dateValue()));
        assertEquals(DateTimeUtil.dateTimeToISO8601UTCString(SOME_DATETIME_VALUE), DateTimeUtil.dateTimeToISO8601UTCString(installationResponse.getCustomAttributeValue(KEY_FOR_DATETIME).dateTimeValue()));
        assertEquals(CustomAttributeValue.Type.String, installationResponse.getCustomAttributeValue(KEY_FOR_STRING).getType());
        assertEquals(CustomAttributeValue.Type.Number, installationResponse.getCustomAttributeValue(KEY_FOR_NUMBER).getType());
        assertEquals(CustomAttributeValue.Type.Date, installationResponse.getCustomAttributeValue(KEY_FOR_DATE).getType());
        assertEquals(CustomAttributeValue.Type.DateTime, installationResponse.getCustomAttributeValue(KEY_FOR_DATETIME).getType());
    }

    @Test
    public void test_validate_list_custom_attributes_map_from_json() {
        Map<String, Object> customAtts = new HashMap<>();

        Map<String, Object> mapForList1 = new HashMap<>();
        mapForList1.put(KEY_FOR_LIST_PARAM_1, 1);
        mapForList1.put(KEY_FOR_LIST_PARAM_2, true);
        Map<String, Object> mapForList2 = new HashMap<>();
        mapForList2.put(KEY_FOR_LIST_PARAM_1, 2);
        mapForList2.put(KEY_FOR_LIST_PARAM_2, false);
        Map<String, Object> mapForList3 = new HashMap<>();
        mapForList3.put(KEY_FOR_LIST_PARAM_1, 2);
        mapForList3.put(KEY_FOR_LIST_PARAM_3, 2);
        Map<String, Object> mapForList4 = new HashMap<>();
        mapForList4.put(KEY_FOR_LIST_PARAM_1, 2);
        mapForList4.put(KEY_FOR_LIST_PARAM_2, "some string");
        Map<String, Object> mapForList5 = new HashMap<>();
        mapForList5.put(KEY_FOR_LIST_PARAM_1, 2);
        mapForList5.put(KEY_FOR_LIST_PARAM_2, false);
        mapForList5.put(KEY_FOR_LIST_PARAM_3, false);

        List<Map<String, Object>> list1 = new LinkedList<>();
        list1.add(mapForList1);
        list1.add(mapForList2);
        customAtts.put(KEY_FOR_LIST, list1);

        assertTrue(CustomAttributesMapper.validate(customAtts));

        list1.add(mapForList3);
        assertFalse(CustomAttributesMapper.validate(customAtts));

        customAtts = new HashMap<>();
        list1 = new LinkedList<>();
        list1.add(mapForList1);
        list1.add(mapForList4);
        customAtts.put(KEY_FOR_LIST, list1);
        assertFalse(CustomAttributesMapper.validate(customAtts));

        list1.remove(mapForList4);
        list1.add(mapForList2);
        list1.add(mapForList5);
        assertFalse(CustomAttributesMapper.validate(customAtts));
    }

    @Test
    public void test_get_custom_attributes_value_from_json_string() {
        UserBody serverResponse = new UserBody();

        User tempUser = new User();
        tempUser = setListCustomAttributes(tempUser);
        Map<String, Object> customAtts = new HashMap<>();
        customAtts.put(KEY_FOR_STRING, SOME_STRING_VALUE);
        customAtts.put(KEY_FOR_EMPTY_STRING, EMPTY_STRING_VALUE);
        customAtts.put(KEY_FOR_DATE, DateTimeUtil.dateToYMDString(SOME_DATE_VALUE));
        customAtts.put(KEY_FOR_DATETIME, DateTimeUtil.dateTimeToISO8601UTCString(SOME_DATETIME_VALUE));
        customAtts.put(KEY_FOR_NUMBER, SOME_NUMBER_VALUE);

        List<Map<String, Object>> list = new LinkedList<>();
        list.add(tempUser.getListCustomAttributeItems(KEY_FOR_LIST).get(0).getMap());
        customAtts.put(KEY_FOR_LIST, list);
        serverResponse.setCustomAttributes(customAtts);

        User user = UserMapper.fromBackend(serverResponse);
        String keyForString = user.getCustomAttributeValue(KEY_FOR_STRING).stringValue();
        String keyForEmptyString = user.getCustomAttributeValue(KEY_FOR_EMPTY_STRING).stringValue();
        Number keyForNumber = user.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = user.getCustomAttributeValue(KEY_FOR_DATE).dateValue();
        CustomAttributeValue.DateTime keyForDateTime = user.getCustomAttributeValue(KEY_FOR_DATETIME).dateTimeValue();
        List<ListCustomAttributeItem> items = user.getListCustomAttributeItems(KEY_FOR_LIST);

        for (ListCustomAttributeItem item: items) {
            assertEquals(SOME_NUMBER_VALUE, item.getNumberValue(KEY_FOR_LIST_PARAM_1).intValue());
            assertEquals(SOME_BOOLEAN_VALUE, item.getBooleanValue(KEY_FOR_LIST_PARAM_2));
            assertEquals(SOME_STRING_VALUE, item.getStringValue(KEY_FOR_LIST_PARAM_3));
            assertEquals(DateTimeUtil.dateToYMDString(SOME_DATE_VALUE), DateTimeUtil.dateToYMDString(item.getDateValue(KEY_FOR_LIST_PARAM_4)));
        }

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(EMPTY_STRING_VALUE, keyForEmptyString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.dateToYMDString(SOME_DATE_VALUE), DateTimeUtil.dateToYMDString(keyForDate));
        assertEquals(DateTimeUtil.dateTimeToISO8601UTCString(SOME_DATETIME_VALUE), DateTimeUtil.dateTimeToISO8601UTCString(keyForDateTime));

        Map<String, CustomAttributeValue> customUserData = user.getCustomAttributes();
        assertEquals(6, customUserData.size());
    }

    @Test
    public void test_get_custom_installation_data_value_from_json_string() {
        AppInstance serverResponse = new AppInstance();
        String expectedDateValue = DateTimeUtil.dateToYMDString(SOME_DATE_VALUE);
        String expectedDateTimeValue = DateTimeUtil.dateTimeToISO8601UTCString(SOME_DATETIME_VALUE);

        Map<String, Object> customAtts = new HashMap<>();
        customAtts.put(KEY_FOR_STRING, SOME_STRING_VALUE);
        customAtts.put(KEY_FOR_EMPTY_STRING, EMPTY_STRING_VALUE);
        customAtts.put(KEY_FOR_DATE, expectedDateValue);
        customAtts.put(KEY_FOR_DATETIME, expectedDateTimeValue);
        customAtts.put(KEY_FOR_NUMBER, SOME_NUMBER_VALUE);
        serverResponse.setCustomAttributes(customAtts);

        Map<String, CustomAttributeValue> customAttsFromBackend = CustomAttributesMapper.customAttsFromBackend(serverResponse.getCustomAttributes());
        String keyForString = customAttsFromBackend.get(KEY_FOR_STRING).stringValue();
        String keyForEmptyString = customAttsFromBackend.get(KEY_FOR_EMPTY_STRING).stringValue();
        Number keyForNumber = customAttsFromBackend.get(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = customAttsFromBackend.get(KEY_FOR_DATE).dateValue();
        CustomAttributeValue.DateTime keyForDateTime = customAttsFromBackend.get(KEY_FOR_DATETIME).dateTimeValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(EMPTY_STRING_VALUE, keyForEmptyString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(expectedDateValue, DateTimeUtil.dateToYMDString(keyForDate));
        assertEquals(expectedDateTimeValue, DateTimeUtil.dateTimeToISO8601UTCString(keyForDateTime));

        assertEquals(5, customAttsFromBackend.size());
    }

    @Test
    public void test_set_multi_custom_attributes_user() throws ParseException {
        Date date = new Date();
        HashMap<String, CustomAttributeValue> customAttsValueHashMap = new HashMap<>();
        customAttsValueHashMap.put(KEY_FOR_STRING, new CustomAttributeValue(SOME_STRING_VALUE));
        customAttsValueHashMap.put(KEY_FOR_EMPTY_STRING, new CustomAttributeValue(EMPTY_STRING_VALUE));
        customAttsValueHashMap.put(KEY_FOR_NUMBER, new CustomAttributeValue(SOME_NUMBER_VALUE));
        customAttsValueHashMap.put(KEY_FOR_DATE, new CustomAttributeValue(date));
        customAttsValueHashMap.put(KEY_FOR_DATETIME, new CustomAttributeValue(SOME_DATETIME_VALUE));

        User user = new User();
        user.setCustomAttributes(customAttsValueHashMap);

        String keyForString = user.getCustomAttributeValue(KEY_FOR_STRING).stringValue();
        String keyForEmptyString = user.getCustomAttributeValue(KEY_FOR_EMPTY_STRING).stringValue();
        Number keyForNumber = user.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = user.getCustomAttributeValue(KEY_FOR_DATE).dateValue();
        CustomAttributeValue.DateTime keyForDateTime = user.getCustomAttributeValue(KEY_FOR_DATETIME).dateTimeValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(EMPTY_STRING_VALUE, keyForEmptyString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.dateToYMDString(date), DateTimeUtil.dateToYMDString(keyForDate));
        assertEquals(DateTimeUtil.dateTimeToISO8601UTCString(SOME_DATETIME_VALUE), DateTimeUtil.dateTimeToISO8601UTCString(keyForDateTime));

        Map<String, CustomAttributeValue> customUserAtts = user.getCustomAttributes();
        assertEquals(5, customUserAtts.size());
    }

    @Test
    public void test_set_multi_custom_attributes_installation() throws ParseException {
        Date date = new Date();
        HashMap<String, CustomAttributeValue> customAttsValueHashMap = new HashMap<>();
        customAttsValueHashMap.put(KEY_FOR_STRING, new CustomAttributeValue(SOME_STRING_VALUE));
        customAttsValueHashMap.put(KEY_FOR_EMPTY_STRING, new CustomAttributeValue(EMPTY_STRING_VALUE));
        customAttsValueHashMap.put(KEY_FOR_NUMBER, new CustomAttributeValue(SOME_NUMBER_VALUE));
        customAttsValueHashMap.put(KEY_FOR_DATE, new CustomAttributeValue(date));
        customAttsValueHashMap.put(KEY_FOR_DATETIME, new CustomAttributeValue(SOME_DATETIME_VALUE));

        Installation installation = new Installation();
        installation.setCustomAttributes(customAttsValueHashMap);

        String keyForString = installation.getCustomAttributeValue(KEY_FOR_STRING).stringValue();
        String keyForEmptyString = installation.getCustomAttributeValue(KEY_FOR_EMPTY_STRING).stringValue();
        Number keyForNumber = installation.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = installation.getCustomAttributeValue(KEY_FOR_DATE).dateValue();
        CustomAttributeValue.DateTime keyForDateTime = installation.getCustomAttributeValue(KEY_FOR_DATETIME).dateTimeValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(EMPTY_STRING_VALUE, keyForEmptyString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.dateToYMDString(date), DateTimeUtil.dateToYMDString(keyForDate));
        assertEquals(DateTimeUtil.dateTimeToISO8601UTCString(SOME_DATETIME_VALUE), DateTimeUtil.dateTimeToISO8601UTCString(keyForDateTime));

        Map<String, CustomAttributeValue> customInstallationAtts = installation.getCustomAttributes();
        assertEquals(5, customInstallationAtts.size());
    }
}
