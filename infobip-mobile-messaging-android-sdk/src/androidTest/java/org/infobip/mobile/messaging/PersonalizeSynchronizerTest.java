package org.infobip.mobile.messaging;


import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.Result;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersonalizeSynchronizerTest extends MobileMessagingTestCase {

    private MobileMessaging.ResultListener<SuccessPending> successPendingResultListener = mock(MobileMessaging.ResultListener.class);
    private ArgumentCaptor<Result> captor = ArgumentCaptor.forClass(Result.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        enableMessageStoreForReceivedMessages();
    }

    @Test
    public void test_depersonalize_without_params_completed() {
        //given
        givenUserData();

        //when
        mobileMessaging.depersonalize();

        //then
        verify(broadcaster, after(300).atLeastOnce()).depersonalized();
        assertFalse(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    @Test
    public void test_depersonalize_without_params_api_error() {

        //given
        doThrow(new ApiIOException("400", "Failed request")).when(mobileApiAppInstance).depersonalize(anyString());
        givenUserData();

        //when
        mobileMessaging.depersonalize();

        //then
        verify(broadcaster, after(300).never()).depersonalized();
        verify(broadcaster, after(300).times(1)).error(any(MobileMessagingError.class));
        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    @Test
    public void test_depersonalize_with_success_pending_listener_completed() {

        //given
        givenUserData();

        //when
        mobileMessaging.depersonalize(successPendingResultListener);

        //then
        verify(broadcaster, after(300).atLeastOnce()).depersonalized();
        verify(broadcaster, after(300).never()).error(any(MobileMessagingError.class));

        verify(successPendingResultListener, after(300).times(1)).onResult(captor.capture());
        Result result = captor.getValue();
        assertNotNull(result.getData());
        assertTrue(result.isSuccess());
        assertNull(result.getError());

        assertFalse(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    @Test
    public void test_depersonalize_with_success_pending_listener_api_error() {

        //given
        doThrow(new ApiIOException("400", "Failed request")).when(mobileApiAppInstance).depersonalize(anyString());
        givenUserData();

        //when
        mobileMessaging.depersonalize(successPendingResultListener);

        //then
        verify(broadcaster, after(300).never()).depersonalized();
        verify(broadcaster, after(300).times(1)).error(any(MobileMessagingError.class));

        verify(successPendingResultListener, after(300).times(1)).onResult(captor.capture());
        Result result = captor.getValue();
        assertNull(result.getData());
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());

        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    private void givenUserData() {
        User user = new User();
        user.setFirstName("John");
        user.setCustomAttributeElement("someKey", new CustomAttributeValue("someValue"));
        HashMap<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("key", "value");
        SystemData systemData = new SystemData("SomeSdkVersion", "SomeOsVersion", "SomeDeviceManufacturer", "SomeDeviceModel", "SomeAppVersion", false, true, true, "SomeLanguage", "SomeDeviceName", "GMT+1");

        PreferenceHelper.saveString(context, MobileMessagingProperty.USER_DATA, user.toString());
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, user.toString());
        PreferenceHelper.saveString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES, new JsonSerializer().serialize(customAttributes));
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES, new JsonSerializer().serialize(customAttributes));
        PreferenceHelper.saveString(context, MobileMessagingProperty.APP_USER_ID, "appUserId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED, true);
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, systemData.toString());
        PreferenceHelper.saveStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, "msgId");
        createMessage(context, "SomeMessageId", true);

        assertEquals(1, MobileMessaging.getInstance(context).getMessageStore().findAll(context).size());
    }

    private void verifyNeededPrefsCleanUp() {
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.USER_DATA));
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_USER_DATA));
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES));
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES));
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.APP_USER_ID));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED));
        assertFalse(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA).isEmpty());
        assertEquals(0, PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS).length);
        assertEquals(0, MobileMessaging.getInstance(context).getMessageStore().findAll(context).size());
    }

    private void verifyPrefs() {
        assertNotNull(PreferenceHelper.findString(context, MobileMessagingProperty.USER_DATA));
        assertNotNull(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_USER_DATA));
        assertNotNull(PreferenceHelper.findString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES));
        assertNotNull(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES));
        assertNotNull(PreferenceHelper.findString(context, MobileMessagingProperty.APP_USER_ID));
        assertTrue(PreferenceHelper.contains(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED));
        assertFalse(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA).isEmpty());
        assertEquals(1, PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS).length);
        assertEquals(1, MobileMessaging.getInstance(context).getMessageStore().findAll(context).size());
    }
}
