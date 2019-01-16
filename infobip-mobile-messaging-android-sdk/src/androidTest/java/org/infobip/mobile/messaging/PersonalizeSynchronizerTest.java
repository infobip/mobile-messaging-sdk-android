package org.infobip.mobile.messaging;


import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;

import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersonalizeSyncronizerTest extends MobileMessagingTestCase {

    private MobileMessaging.ResultListener<SuccessPending> successPendingResultListener = mock(MobileMessaging.ResultListener.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        enableMessageStoreForReceivedMessages();
    }

    @Test
    public void test_depersonalize_without_params_completed() throws Exception {
        //given
//        given(mobileApiAppInstance.depersonalize(anyString())).willReturn(null);
        givenUserData();

        //when
        mobileMessaging.depersonalize();

        //then
        verify(broadcaster, after(500).atLeastOnce()).depersonalized();
        assertFalse(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_LOGOUT_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    @Test
    public void test_depersonalize_without_params_api_error() throws Exception {

        //given
        doThrow(new ApiIOException("400", "Failed request")).when(mobileApiAppInstance).depersonalize(anyString());
        givenUserData();

        //when
        mobileMessaging.depersonalize();

        //then
        verify(broadcaster, after(500).never()).depersonalized();
        verify(broadcaster, after(500).never()).error(any(MobileMessagingError.class));
        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_LOGOUT_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    @Test
    public void test_depersonalize_with_success_pending_listener_completed() throws Exception {

        //given
//        given(mobileApiAppInstance.depersonalize(anyString())).willReturn(null);
        givenUserData();

        //when
        mobileMessaging.depersonalize(successPendingResultListener);

        //then
        verify(broadcaster, after(500).atLeastOnce()).depersonalized();
        verify(broadcaster, after(500).never()).error(any(MobileMessagingError.class));
        verify(successPendingResultListener, after(500).atLeastOnce()).onResult(any(SuccessPending.class));
        verify(successPendingResultListener, after(500).never()).onError(any(MobileMessagingError.class));
        assertFalse(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_LOGOUT_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    @Test
    public void test_depersonalize_with_success_pending_listener_api_error() throws Exception {

        //given
        doThrow(new ApiIOException("400", "Failed request")).when(mobileApiAppInstance).depersonalize(anyString());
        givenUserData();

        //when
        mobileMessaging.depersonalize(successPendingResultListener);

        //then
        verify(broadcaster, after(500).never()).depersonalized();
        verify(broadcaster, after(500).never()).error(any(MobileMessagingError.class));
        verify(successPendingResultListener, after(500).never()).onResult(any(SuccessPending.class));
        verify(successPendingResultListener, after(500).atLeastOnce()).onError(any(MobileMessagingError.class));
        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_LOGOUT_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    @Test
    public void test_depersonalize_with_push_reg_id_and_success_pending_listener_completed() throws Exception {

        //given
        String givenRegId = "depersonalizePushRegId";
//        given(mobileApiAppInstance.depersonalize(givenRegId)).willReturn(null);
        givenUserData();

        //when
        mobileMessaging.depersonalize(eq(givenRegId), successPendingResultListener);

        //then
        verify(broadcaster, after(500).atLeastOnce()).depersonalized();
        verify(broadcaster, after(500).never()).error(any(MobileMessagingError.class));
        verify(successPendingResultListener, after(500).atLeastOnce()).onResult(any(SuccessPending.class));
        verify(successPendingResultListener, after(500).never()).onError(any(MobileMessagingError.class));
        assertFalse(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_LOGOUT_UNREPORTED));
        verifyPrefs();
    }

    @Test
    public void test_depersonalize_with_push_reg_id_and_success_pending_listener_api_error() throws Exception {

        //given
        String givenRegId = "depersonalizePushRegId";
        doThrow(new ApiIOException("400", "Failed request")).when(mobileApiAppInstance).depersonalize(givenRegId);
        givenUserData();

        //when
        mobileMessaging.depersonalize(givenRegId, successPendingResultListener);

        //then
        verify(broadcaster, after(500).never()).depersonalized();
        verify(broadcaster, after(500).never()).error(any(MobileMessagingError.class));
        verify(successPendingResultListener, after(500).never()).onResult(any(SuccessPending.class));
        verify(successPendingResultListener, after(500).atLeastOnce()).onError(any(MobileMessagingError.class));
        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_LOGOUT_UNREPORTED));
        verifyPrefs();
    }

    private void givenUserData() {
        UserData userData = new UserData();
        userData.setFirstName("John");
        userData.setCustomUserDataElement("someKey", new CustomUserDataValue("someValue"));
        HashMap<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("key", "value");
        SystemData systemData = new SystemData("SomeSdkVersion", "SomeOsVersion", "SomeDeviceManufacturer", "SomeDeviceModel", "SomeAppVersion", false, true, true, "SomeOsLanguage", "SomeDeviceName");

        PreferenceHelper.saveString(context, MobileMessagingProperty.USER_DATA, userData.toString());
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, userData.toString());
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
