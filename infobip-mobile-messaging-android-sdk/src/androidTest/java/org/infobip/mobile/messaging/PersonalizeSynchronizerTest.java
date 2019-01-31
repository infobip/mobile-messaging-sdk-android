package org.infobip.mobile.messaging;


import org.infobip.mobile.messaging.api.appinstance.UserPersonalizeBody;
import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.Result;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersonalizeSynchronizerTest extends MobileMessagingTestCase {

    private MobileMessaging.ResultListener<SuccessPending> successPendingResultListener = mock(MobileMessaging.ResultListener.class);
    private MobileMessaging.ResultListener<User> userResultListener = mock(MobileMessaging.ResultListener.class);
    private ArgumentCaptor<Result> captor = ArgumentCaptor.forClass(Result.class);
    private ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        enableMessageStoreForReceivedMessages();
    }

    @Test
    public void test_personalize_without_user_atts_with_force_depersonalize_completed() {
        //given
        givenUserData();
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId("extId");
        userIdentity.setPhones(CollectionUtils.setOf("111", "222"));
        userIdentity.setEmails(CollectionUtils.setOf("email1@mail.com", "email2@mail.com"));

        //when
        mobileMessaging.personalize(userIdentity, null, true, userResultListener);

        //then
        verify(broadcaster, after(300).atLeastOnce()).personalized(userCaptor.capture());
        User returnedUser = userCaptor.getValue();
        assertEquals(userIdentity.getEmails(), returnedUser.getEmails());
        assertEquals(userIdentity.getPhones(), returnedUser.getPhones());
        assertEquals(userIdentity.getExternalUserId(), returnedUser.getExternalUserId());
        assertNull(returnedUser.getFirstName());
        assertNull(returnedUser.getCustomAttributes());
        assertNull(returnedUser.getTags());
        assertNull(returnedUser.getGender());
        assertNull(returnedUser.getBirthday());
        assertNull(returnedUser.getFirstName());
        assertNull(returnedUser.getMiddleName());
        verifyNeededPrefsCleanUp(false);
    }

    @Test
    public void test_personalize_with_user_atts_with_force_depersonalize_completed() {
        //given
        givenUserData();
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId("extId");
        userIdentity.setPhones(CollectionUtils.setOf("111", "222"));
        userIdentity.setEmails(CollectionUtils.setOf("email1@mail.com", "email2@mail.com"));
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFirstName("Darth");
        userAttributes.setLastName("Vader");
        userAttributes.setMiddleName("Beloved");
        userAttributes.setGender(UserAttributes.Gender.Male);

        //when
        mobileMessaging.personalize(userIdentity, userAttributes, true, userResultListener);

        //then
        verify(broadcaster, after(300).atLeastOnce()).personalized(userCaptor.capture());
        User returnedUser = userCaptor.getValue();
        assertEquals(userIdentity.getEmails(), returnedUser.getEmails());
        assertEquals(userIdentity.getPhones(), returnedUser.getPhones());
        assertEquals(userIdentity.getExternalUserId(), returnedUser.getExternalUserId());
        assertEquals(userAttributes.getFirstName(), returnedUser.getFirstName());
        assertEquals(userAttributes.getLastName(), returnedUser.getLastName());
        assertEquals(userAttributes.getMiddleName(), returnedUser.getMiddleName());
        assertEquals(userAttributes.getGender(), returnedUser.getGender());
        assertNull(returnedUser.getCustomAttributes());
        assertNull(returnedUser.getTags());
        assertNull(returnedUser.getBirthday());
        verifyNeededPrefsCleanUp(false);
    }

    @Test
    public void test_personalize_without_force_depersonalize_completed() {
        //given
        givenUserData();

        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId("extId");
        userIdentity.setPhones(CollectionUtils.setOf("111", "222"));
        userIdentity.setEmails(CollectionUtils.setOf("email1@mail.com", "email2@mail.com"));

        //when
        mobileMessaging.personalize(userIdentity, null, false, userResultListener);

        //then
        verify(broadcaster, after(300).atLeastOnce()).personalized(userCaptor.capture());
        User returnedUser = userCaptor.getValue();
        assertEquals(userIdentity.getEmails(), returnedUser.getEmails());
        assertEquals(userIdentity.getPhones(), returnedUser.getPhones());
        assertEquals(userIdentity.getExternalUserId(), returnedUser.getExternalUserId());
        assertEquals("John", returnedUser.getFirstName());
        assertEquals(new Date(1999, 1, 1), returnedUser.getBirthday());
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
    public void test_personalize_with_force_depersonalize_api_error() {

        //given
        doThrow(new ApiIOException(ApiErrorCode.PERSONALIZATION_IMPOSSIBLE, "Personalize impossible"))
                .when(mobileApiAppInstance).personalize(anyString(), anyBoolean(), any(UserPersonalizeBody.class));

        //when
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId("extUserId");
        mobileMessaging.personalize(userIdentity, new UserAttributes(), true);

        //then
        verify(broadcaster, after(300).never()).personalized(any(User.class));
        verify(broadcaster, after(300).times(1)).error(any(MobileMessagingError.class));
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
        user.setBirthday(new Date(1999, 1, 1));
        user.setCustomAttribute("someKey", new CustomAttributeValue("someValue"));
        SystemData systemData = new SystemData("SomeSdkVersion", "SomeOsVersion", "SomeDeviceManufacturer", "SomeDeviceModel", "SomeAppVersion", false, true, true, "SomeLanguage", "SomeDeviceName", "GMT+1");

        String savedUser = UserMapper.toJson(user);
        PreferenceHelper.saveString(context, MobileMessagingProperty.USER_DATA, savedUser);
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, savedUser);
        PreferenceHelper.saveString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES, new JsonSerializer().serialize(user.getCustomAttributes()));
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES, new JsonSerializer().serialize(user.getCustomAttributes()));
        PreferenceHelper.saveString(context, MobileMessagingProperty.APP_USER_ID, "appUserId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED, true);
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, systemData.toString());
        PreferenceHelper.saveStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, "msgId");
        createMessage(context, "SomeMessageId", true);

        assertEquals(1, MobileMessaging.getInstance(context).getMessageStore().findAll(context).size());
    }

    private void verifyNeededPrefsCleanUp(boolean noUserData) {
        if (noUserData) {
            assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.USER_DATA));
        }
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_USER_DATA));
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES));
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES));
        assertNull(PreferenceHelper.findString(context, MobileMessagingProperty.APP_USER_ID));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED));
        assertEquals(0, PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS).length);
        assertEquals(0, MobileMessaging.getInstance(context).getMessageStore().findAll(context).size());
    }

    private void verifyNeededPrefsCleanUp() {
        verifyNeededPrefsCleanUp(true);
    }
}
