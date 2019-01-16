package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

public class CustomUserDataTypeTest extends MobileMessagingTestCase {

    private ArgumentCaptor<UserData> captor;

    private static final String KEY_FOR_STRING = "keyForString";
    private static final String KEY_FOR_NUMBER = "keyForNumber";
    private static final String KEY_FOR_DATE = "keyForDate";

    private final String SOME_STRING_VALUE = "bla";
    private final int SOME_NUMBER_VALUE = 1111;
    private final Date SOME_DATE_VALUE = new Date();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        captor = ArgumentCaptor.forClass(UserData.class);
    }

    @Test
    public void test_sync_user_data() {
        UserData userData = new UserData();
        userData.setCustomUserDataElement(KEY_FOR_STRING, new CustomUserDataValue(SOME_STRING_VALUE));
        userData.setCustomUserDataElement(KEY_FOR_NUMBER, new CustomUserDataValue(SOME_NUMBER_VALUE));
        userData.setCustomUserDataElement(KEY_FOR_DATE, new CustomUserDataValue(SOME_DATE_VALUE));
        mobileMessaging.saveUser(userData);

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userDataReported(captor.capture());
        UserData userDataResponse = captor.getValue();
        assertEquals(SOME_STRING_VALUE, userDataResponse.getCustomUserDataValue(KEY_FOR_STRING).stringValue());
        assertEquals(SOME_NUMBER_VALUE, userDataResponse.getCustomUserDataValue(KEY_FOR_NUMBER).numberValue().intValue());
        assertEquals(DateTimeUtil.DateToYMDString(SOME_DATE_VALUE), DateTimeUtil.DateToYMDString(userDataResponse.getCustomUserDataValue(KEY_FOR_DATE).dateValue()));
        assertEquals(CustomUserDataValue.Type.String, userDataResponse.getCustomUserDataValue(KEY_FOR_STRING).getType());
        assertEquals(CustomUserDataValue.Type.Number, userDataResponse.getCustomUserDataValue(KEY_FOR_NUMBER).getType());
        assertEquals(CustomUserDataValue.Type.Date, userDataResponse.getCustomUserDataValue(KEY_FOR_DATE).getType());
    }

    @Test
    public void test_get_custom_user_data_value_from_json_string() {
        UserBody serverResponse = new UserBody();

        Map<String, Object> customAtts = new HashMap<>();
        customAtts.put(KEY_FOR_STRING, SOME_STRING_VALUE);
        customAtts.put(KEY_FOR_DATE, DateTimeUtil.DateToYMDString(SOME_DATE_VALUE));
        customAtts.put(KEY_FOR_NUMBER, SOME_NUMBER_VALUE);
        serverResponse.setCustomAttributes(customAtts);

        UserData userData = UserDataMapper.fromBackend(serverResponse);
        String keyForString = userData.getCustomUserDataValue(KEY_FOR_STRING).stringValue();
        Number keyForNumber = userData.getCustomUserDataValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = userData.getCustomUserDataValue(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.DateToYMDString(SOME_DATE_VALUE), DateTimeUtil.DateToYMDString(keyForDate));

        Map<String, CustomUserDataValue> customUserData = userData.getCustomAttributes();
        assertEquals(3, customUserData.size());
    }

    @Test
    public void test_get_custom_installation_data_value_from_json_string() {
        AppInstance serverResponse = new AppInstance();

        Map<String, Object> customAtts = new HashMap<>();
        customAtts.put(KEY_FOR_STRING, SOME_STRING_VALUE);
        customAtts.put(KEY_FOR_DATE, DateTimeUtil.DateToYMDString(SOME_DATE_VALUE));
        customAtts.put(KEY_FOR_NUMBER, SOME_NUMBER_VALUE);
        serverResponse.setCustomAttributes(customAtts);

        Map<String, CustomUserDataValue> customAttsFromBackend = UserDataMapper.customAttsFromBackend(serverResponse.getCustomAttributes());
        String keyForString = customAttsFromBackend.get(KEY_FOR_STRING).stringValue();
        Number keyForNumber = customAttsFromBackend.get(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = customAttsFromBackend.get(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.DateToYMDString(SOME_DATE_VALUE), DateTimeUtil.DateToYMDString(keyForDate));

        assertEquals(3, customAttsFromBackend.size());
    }

    @Test
    public void test_set_multi_custom_user_data() throws ParseException {
        Date date = new Date();
        HashMap<String, CustomUserDataValue> userDataValueHashMap = new HashMap<>();
        userDataValueHashMap.put(KEY_FOR_STRING, new CustomUserDataValue(SOME_STRING_VALUE));
        userDataValueHashMap.put(KEY_FOR_NUMBER, new CustomUserDataValue(SOME_NUMBER_VALUE));
        userDataValueHashMap.put(KEY_FOR_DATE, new CustomUserDataValue(date));

        UserData userData = new UserData();
        userData.setCustomAttributes(userDataValueHashMap);

        String keyForString = userData.getCustomUserDataValue(KEY_FOR_STRING).stringValue();
        Number keyForNumber = userData.getCustomUserDataValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = userData.getCustomUserDataValue(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.DateToYMDString(date), DateTimeUtil.DateToYMDString(keyForDate));

        Map<String, CustomUserDataValue> customUserData = userData.getCustomAttributes();
        assertEquals(3, customUserData.size());
    }
}
