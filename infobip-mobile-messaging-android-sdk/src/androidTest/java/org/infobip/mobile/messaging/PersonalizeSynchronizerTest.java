package org.infobip.mobile.messaging;


import android.support.annotation.NonNull;

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

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

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

    private String givenFirstName = "John";
    private Date givenBirthday = new Date(1999, 1, 1);
    private String givenExternalUserId = "extId";
    private Set<String> givenPhones = CollectionUtils.setOf("111", "222");
    private Set<String> givenEmails = CollectionUtils.setOf("email1@mail.com", "email2@mail.com");
    private Map<String, CustomAttributeValue> givenCustomAtts = Collections.singletonMap("someKey", new CustomAttributeValue("someValue"));

    @Override
    public void setUp() throws Exception {
        super.setUp();

        enableMessageStoreForReceivedMessages();
    }

    @Test
    public void test_personalize_without_user_atts_with_force_depersonalize_completed() throws Exception {
        //given
        givenUserData();
        UserIdentity userIdentity = givenIdentity();

        //when
        mobileMessaging.personalize(userIdentity, null, true, userResultListener);

        //then
        verifyNeededPrefsCleanUp(false);
        verify(broadcaster, after(300).atLeastOnce()).personalized(userCaptor.capture());
        User returnedUser = userCaptor.getValue();
        verifyIdentity(returnedUser);
        assertNull(returnedUser.getFirstName());
        assertNull(returnedUser.getCustomAttributes());
        assertNull(returnedUser.getTags());
        assertNull(returnedUser.getGender());
        assertNull(returnedUser.getBirthday());
        assertNull(returnedUser.getFirstName());
        assertNull(returnedUser.getMiddleName());

        assertJEquals(returnedUser, mobileMessagingCore.getUser());
    }

    @Test
    public void test_personalize_with_user_atts_with_force_depersonalize_completed() throws Exception {
        //given
        givenUserData();
        UserIdentity userIdentity = givenIdentity();
        String newFirstName = "Darth";
        String newLastName = "Vader";
        String newMiddleName = "Beloved";
        UserAttributes.Gender newGender = UserAttributes.Gender.Male;

        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFirstName(newFirstName);
        userAttributes.setLastName(newLastName);
        userAttributes.setMiddleName(newMiddleName);
        userAttributes.setGender(newGender);

        //when
        mobileMessaging.personalize(userIdentity, userAttributes, true, userResultListener);

        //then
        verifyNeededPrefsCleanUp(false);
        verify(broadcaster, after(300).atLeastOnce()).personalized(userCaptor.capture());
        User returnedUser = userCaptor.getValue();
        verifyIdentity(returnedUser);
        assertEquals(newFirstName, returnedUser.getFirstName());
        assertEquals(newLastName, returnedUser.getLastName());
        assertEquals(newMiddleName, returnedUser.getMiddleName());
        assertEquals(newGender, returnedUser.getGender());
        assertNull(returnedUser.getCustomAttributes());
        assertNull(returnedUser.getTags());
        assertNull(returnedUser.getBirthday());

        assertJEquals(returnedUser, mobileMessagingCore.getUser());
    }

    @Test
    public void test_personalize_without_force_depersonalize_completed() throws Exception {
        //given
        givenUserData();
        UserIdentity userIdentity = givenIdentity();

        //when
        mobileMessaging.personalize(userIdentity, null, false, userResultListener);

        //then
        verify(broadcaster, after(300).atLeastOnce()).personalized(userCaptor.capture());
        User returnedUser = userCaptor.getValue();
        verifyIdentity(returnedUser);
        assertEquals(givenFirstName, returnedUser.getFirstName());
        assertEquals(givenBirthday, returnedUser.getBirthday());
        assertJEquals(givenCustomAtts, returnedUser.getCustomAttributes());

        assertJEquals(returnedUser, mobileMessagingCore.getUser());
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
        assertNull(mobileMessagingCore.getUser());
    }

    @Test
    public void test_personalize_with_force_depersonalize_personalization_impossible_api_error() {

        //given
        doThrow(new ApiIOException(ApiErrorCode.PERSONALIZATION_IMPOSSIBLE, "Personalize impossible"))
                .when(mobileApiAppInstance).personalize(anyString(), anyBoolean(), any(UserPersonalizeBody.class));
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId(givenExternalUserId);

        //when
        mobileMessaging.personalize(userIdentity, new UserAttributes(), true);

        //then
        verify(broadcaster, after(300).never()).personalized(any(User.class));
        verify(broadcaster, after(300).times(1)).error(any(MobileMessagingError.class));
        verifyNeededPrefsCleanUp();
        assertNull(mobileMessagingCore.getUser());
    }

    @Test
    public void test_personalize_with_force_depersonalize_ambiguous_personalize_candidates_api_error() {

        //given
        doThrow(new ApiIOException(ApiErrorCode.AMBIGUOUS_PERSONALIZE_CANDIDATES, "Ambiguous personalize candidates"))
                .when(mobileApiAppInstance).personalize(anyString(), anyBoolean(), any(UserPersonalizeBody.class));
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId(givenExternalUserId);

        //when
        mobileMessaging.personalize(userIdentity, new UserAttributes(), true);

        //then
        verify(broadcaster, after(300).never()).personalized(any(User.class));
        verify(broadcaster, after(300).times(1)).error(any(MobileMessagingError.class));
        verifyNeededPrefsCleanUp();
        assertNull(mobileMessagingCore.getUser());
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
        assertNull(mobileMessagingCore.getUser());
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
        assertNull(mobileMessagingCore.getUser());
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
        assertNull(mobileMessagingCore.getUser());

        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED));
        verifyNeededPrefsCleanUp();
    }

    private void givenUserData() {
        User user = new User();
        user.setFirstName(givenFirstName);
        user.setBirthday(givenBirthday);
        user.setCustomAttributes(givenCustomAtts);
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

    @NonNull
    private UserIdentity givenIdentity() {
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId(givenExternalUserId);
        userIdentity.setPhones(givenPhones);
        userIdentity.setEmails(givenEmails);
        return userIdentity;
    }

    private void verifyIdentity(User returnedUser) {
        assertEquals(givenEmails, returnedUser.getEmails());
        assertEquals(givenPhones, returnedUser.getPhones());
        assertEquals(givenExternalUserId, returnedUser.getExternalUserId());
    }
}
