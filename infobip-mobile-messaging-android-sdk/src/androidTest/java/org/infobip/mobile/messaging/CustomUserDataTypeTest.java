package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class CustomUserDataTypeTest extends MobileMessagingTestCase {

    ArgumentCaptor<UserData> captor;

    private static final String KEY_FOR_STRING = "keyForString";
    private static final String KEY_FOR_NUMBER = "keyForNumber";
    private static final String KEY_FOR_DATE = "keyForDate";

    private final String SOME_STRING_VALUE = "bla";
    private final int SOME_NUMBER_VALUE = 1111;
    private final Date SOME_DATE_VALUE = new Date();

    private String serverResponse = "{\n" +
            "  \"customUserData\": {\n" +
            "    \"" + KEY_FOR_DATE + "\": {\n" +
            "      \"type\": \"Date\",\n" +
            "      \"value\": \"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(SOME_DATE_VALUE) + "\"\n" +
            "    },\n" +
            "    \"" + KEY_FOR_NUMBER + "\": {\n" +
            "      \"type\": \"Number\",\n" +
            "      \"value\": 1111\n" +
            "    },\n" +
            "    \"" + KEY_FOR_STRING + "\": {\n" +
            "      \"type\": \"String\",\n" +
            "      \"value\": \"bla\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"externalUserId\": null,\n" +
            "  \"predefinedUserData\": {}\n" +
            "}";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        captor = ArgumentCaptor.forClass(UserData.class);
    }

    public void test_sync_user_data() {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, serverResponse);

        UserData userData = new UserData();
        userData.setCustomUserDataElement(KEY_FOR_STRING, new CustomUserDataValue(SOME_STRING_VALUE));
        userData.setCustomUserDataElement(KEY_FOR_NUMBER, new CustomUserDataValue(SOME_NUMBER_VALUE));
        userData.setCustomUserDataElement(KEY_FOR_DATE, new CustomUserDataValue(SOME_DATE_VALUE));
        mobileMessaging.syncUserData(userData);

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).userDataReported(captor.capture());
        UserData userDataResponse = captor.getValue();
        assertEquals(SOME_STRING_VALUE, userDataResponse.getCustomUserDataValue(KEY_FOR_STRING).stringValue());
        assertEquals(SOME_NUMBER_VALUE, userDataResponse.getCustomUserDataValue(KEY_FOR_NUMBER).numberValue().intValue());
        assertEquals(SOME_DATE_VALUE.toString(), userDataResponse.getCustomUserDataValue(KEY_FOR_DATE).dateValue().toString());
        assertEquals(CustomUserDataValue.Type.String, userDataResponse.getCustomUserDataValue(KEY_FOR_STRING).getType());
        assertEquals(CustomUserDataValue.Type.Number, userDataResponse.getCustomUserDataValue(KEY_FOR_NUMBER).getType());
        assertEquals(CustomUserDataValue.Type.Date, userDataResponse.getCustomUserDataValue(KEY_FOR_DATE).getType());
    }

    public void test_get_custom_user_data_value_from_json_string() throws ParseException {
        UserData userData = new UserData(serverResponse);
        String keyForString = userData.getCustomUserDataValue(KEY_FOR_STRING).stringValue();
        Number keyForNumber = userData.getCustomUserDataValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = userData.getCustomUserDataValue(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(SOME_DATE_VALUE.toString(), keyForDate.toString());

        Map<String, CustomUserDataValue> customUserData = userData.getCustomUserData();
        assertEquals(3, customUserData.size());
    }

    public void test_set_multi_custom_user_data() throws ParseException {
        Date date = new Date();
        HashMap<String, CustomUserDataValue> userDataValueHashMap = new HashMap<>();
        userDataValueHashMap.put(KEY_FOR_STRING, new CustomUserDataValue(SOME_STRING_VALUE));
        userDataValueHashMap.put(KEY_FOR_NUMBER, new CustomUserDataValue(SOME_NUMBER_VALUE));
        userDataValueHashMap.put(KEY_FOR_DATE, new CustomUserDataValue(date));

        UserData userData = new UserData();
        userData.setCustomUserData(userDataValueHashMap);

        String keyForString = userData.getCustomUserDataValue(KEY_FOR_STRING).stringValue();
        Number keyForNumber = userData.getCustomUserDataValue(KEY_FOR_NUMBER).numberValue();
        Date keyForDate = userData.getCustomUserDataValue(KEY_FOR_DATE).dateValue();

        assertEquals(SOME_STRING_VALUE, keyForString);
        assertEquals(SOME_NUMBER_VALUE, keyForNumber.intValue());
        assertEquals(date.toString(), keyForDate.toString());

        Map<String, CustomUserDataValue> customUserData = userData.getCustomUserData();
        assertEquals(3, customUserData.size());
    }
}
