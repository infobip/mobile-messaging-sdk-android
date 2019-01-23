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

public class CustomAttributeTypeTest extends MobileMessagingTestCase {

    private ArgumentCaptor<User> userCaptor;
    private ArgumentCaptor<Installation> installationCaptor;

    private static final String KEY_FOR_STRING = "keyForString";
    private static final String KEY_FOR_NUMBER = "keyForNumber";
    private static final String KEY_FOR_DATE = "keyForDate";

    private final String SOME_STRING_VALUE = "bla";
    private final int SOME_NUMBER_VALUE = 1111;
    private final Date SOME_DATE_VALUE = new Date();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        userCaptor = ArgumentCaptor.forClass(User.class);
        installationCaptor = ArgumentCaptor.forClass(Installation.class);
    }

    @Test
    public void test_save_user_custom_attributes() {
        User user = new User();
        user.setCustomAttribute(KEY_FOR_STRING, new CustomAttributeValue(SOME_STRING_VALUE));
        user.setCustomAttribute(KEY_FOR_NUMBER, new CustomAttributeValue(SOME_NUMBER_VALUE));
        user.setCustomAttribute(KEY_FOR_DATE, new CustomAttributeValue(SOME_DATE_VALUE));
        mobileMessaging.saveUser(user);

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userUpdated(userCaptor.capture());
        User userResponse = userCaptor.getValue();
        assertEquals(SOME_STRING_VALUE, userResponse.getCustomAttributeValue(KEY_FOR_STRING).stringValue());
        assertEquals(SOME_NUMBER_VALUE, userResponse.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue().intValue());
        assertEquals(DateTimeUtil.DateToYMDString(SOME_DATE_VALUE), DateTimeUtil.DateToYMDString(userResponse.getCustomAttributeValue(KEY_FOR_DATE).dateValue()));
        assertEquals(CustomAttributeValue.Type.String, userResponse.getCustomAttributeValue(KEY_FOR_STRING).getType());
        assertEquals(CustomAttributeValue.Type.Number, userResponse.getCustomAttributeValue(KEY_FOR_NUMBER).getType());
        assertEquals(CustomAttributeValue.Type.Date, userResponse.getCustomAttributeValue(KEY_FOR_DATE).getType());
    }

    @Test
    public void test_save_installation_custom_attributes() {
        Installation installation = new Installation();
        installation.setCustomAttribute(KEY_FOR_STRING, new CustomAttributeValue(SOME_STRING_VALUE));
        installation.setCustomAttribute(KEY_FOR_NUMBER, new CustomAttributeValue(SOME_NUMBER_VALUE));
        installation.setCustomAttribute(KEY_FOR_DATE, new CustomAttributeValue(SOME_DATE_VALUE));
        mobileMessaging.saveInstallation(installation);

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).installationUpdated(installationCaptor.capture());
        Installation installationResponse = installationCaptor.getValue();
        assertEquals(SOME_STRING_VALUE, installationResponse.getCustomAttributeValue(KEY_FOR_STRING).stringValue());
        assertEquals(SOME_NUMBER_VALUE, installationResponse.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue().intValue());
        assertEquals(DateTimeUtil.DateToYMDString(SOME_DATE_VALUE), DateTimeUtil.DateToYMDString(installationResponse.getCustomAttributeValue(KEY_FOR_DATE).dateValue()));
        assertEquals(CustomAttributeValue.Type.String, installationResponse.getCustomAttributeValue(KEY_FOR_STRING).getType());
        assertEquals(CustomAttributeValue.Type.Number, installationResponse.getCustomAttributeValue(KEY_FOR_NUMBER).getType());
        assertEquals(CustomAttributeValue.Type.Date, installationResponse.getCustomAttributeValue(KEY_FOR_DATE).getType());
    }

    @Test
    public void test_get_custom_attributes_value_from_json_string() {
        UserBody serverResponse = new UserBody();

        Map<String, Object> customAtts = new HashMap<>();
        customAtts.put(KEY_FOR_STRING, SOME_STRING_VALUE);
        customAtts.put(KEY_FOR_DATE, DateTimeUtil.DateToYMDString(SOME_DATE_VALUE));
        customAtts.put(KEY_FOR_NUMBER, SOME_NUMBER_VALUE);
        serverResponse.setCustomAttributes(customAtts);

        User user = UserMapper.fromBackend(serverResponse);
        String keyForString = user.getCustomAttributeValue(KEY_FOR_STRING).stringValue();
        Number keyForNumber = user.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = user.getCustomAttributeValue(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.DateToYMDString(SOME_DATE_VALUE), DateTimeUtil.DateToYMDString(keyForDate));

        Map<String, CustomAttributeValue> customUserData = user.getCustomAttributes();
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

        Map<String, CustomAttributeValue> customAttsFromBackend = UserMapper.customAttsFromBackend(serverResponse.getCustomAttributes());
        String keyForString = customAttsFromBackend.get(KEY_FOR_STRING).stringValue();
        Number keyForNumber = customAttsFromBackend.get(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = customAttsFromBackend.get(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.DateToYMDString(SOME_DATE_VALUE), DateTimeUtil.DateToYMDString(keyForDate));

        assertEquals(3, customAttsFromBackend.size());
    }

    @Test
    public void test_set_multi_custom_attributes_user() throws ParseException {
        Date date = new Date();
        HashMap<String, CustomAttributeValue> customAttsValueHashMap = new HashMap<>();
        customAttsValueHashMap.put(KEY_FOR_STRING, new CustomAttributeValue(SOME_STRING_VALUE));
        customAttsValueHashMap.put(KEY_FOR_NUMBER, new CustomAttributeValue(SOME_NUMBER_VALUE));
        customAttsValueHashMap.put(KEY_FOR_DATE, new CustomAttributeValue(date));

        User user = new User();
        user.setCustomAttributes(customAttsValueHashMap);

        String keyForString = user.getCustomAttributeValue(KEY_FOR_STRING).stringValue();
        Number keyForNumber = user.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = user.getCustomAttributeValue(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.DateToYMDString(date), DateTimeUtil.DateToYMDString(keyForDate));

        Map<String, CustomAttributeValue> customUserAtts = user.getCustomAttributes();
        assertEquals(3, customUserAtts.size());
    }

    @Test
    public void test_set_multi_custom_attributes_installation() throws ParseException {
        Date date = new Date();
        HashMap<String, CustomAttributeValue> customAttsValueHashMap = new HashMap<>();
        customAttsValueHashMap.put(KEY_FOR_STRING, new CustomAttributeValue(SOME_STRING_VALUE));
        customAttsValueHashMap.put(KEY_FOR_NUMBER, new CustomAttributeValue(SOME_NUMBER_VALUE));
        customAttsValueHashMap.put(KEY_FOR_DATE, new CustomAttributeValue(date));

        Installation installation = new Installation();
        installation.setCustomAttributes(customAttsValueHashMap);

        String keyForString = installation.getCustomAttributeValue(KEY_FOR_STRING).stringValue();
        Number keyForNumber = installation.getCustomAttributeValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = installation.getCustomAttributeValue(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(DateTimeUtil.DateToYMDString(date), DateTimeUtil.DateToYMDString(keyForDate));

        Map<String, CustomAttributeValue> customInstallationAtts = installation.getCustomAttributes();
        assertEquals(3, customInstallationAtts.size());
    }
}
