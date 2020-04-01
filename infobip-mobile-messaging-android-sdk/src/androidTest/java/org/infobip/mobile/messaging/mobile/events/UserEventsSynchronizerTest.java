package org.infobip.mobile.messaging.mobile.events;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.CustomEvent;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserCustomEventBody;
import org.infobip.mobile.messaging.api.appinstance.UserSessionEventBody;
import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class UserEventsSynchronizerTest extends MobileMessagingTestCase {

    private UserEventsSynchronizer userEventsSynchronizer;

    private MobileApiAppInstance mobileApiAppInstance = mock(MobileApiAppInstance.class);
    private MobileMessaging.ResultListener<CustomEvent> eventResultListener = mock(MobileMessaging.ResultListener.class);
    private ArgumentCaptor<UserCustomEventBody> customEventCaptor;
    private ArgumentCaptor<UserSessionEventBody> userSessionEventCaptor;
    private Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
    private Executor executor = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    };
    private final long date = System.currentTimeMillis();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        customEventCaptor = ArgumentCaptor.forClass(UserCustomEventBody.class);
        userSessionEventCaptor = ArgumentCaptor.forClass(UserSessionEventBody.class);
        RetryPolicyProvider retryPolicyProvider = new RetryPolicyProvider(context);
        MRetryPolicy retryPolicy = retryPolicyProvider.DEFAULT();
        userEventsSynchronizer = new UserEventsSynchronizer(mobileMessagingCore, broadcaster, mobileApiAppInstance, retryPolicy, executor, new BatchReporter(100L));
    }

    @Test
    public void shouldReportSessionEventsOnServer() throws Exception {
        long sessionEndMillis = Time.now();
        long sessionStartMillis = sessionEndMillis - 60 * 1000;
        PreferenceHelper.saveLong(context, MobileMessagingProperty.ACTIVE_SESSION_START_TIME_MILLIS, sessionStartMillis);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.ACTIVE_SESSION_END_TIME_MILLIS, sessionEndMillis);
        mobileMessagingCore.saveSessionBounds(context, sessionStartMillis, sessionEndMillis);
        HashMap<String, String> sessionBounds = UserEventsRequestMapper.getSessionBounds(mobileMessagingCore.getStoredSessionBounds());

        assertEquals(sessionStartMillis, mobileMessagingCore.getActiveSessionStartTime());
        assertEquals(sessionEndMillis, mobileMessagingCore.getActiveSessionEndTime());
        assertEquals(1, sessionBounds.size());

        userEventsSynchronizer.reportSessions();

        verify(broadcaster, after(300).times(1)).userSessionsReported();
        verify(mobileApiAppInstance, times(1)).sendUserSessionReport(anyString(), userSessionEventCaptor.capture());
        assertEquals(sessionStartMillis, mobileMessagingCore.getActiveSessionStartTime());
        assertEquals(sessionEndMillis, mobileMessagingCore.getActiveSessionEndTime());
        assertEquals(0, UserEventsRequestMapper.getSessionBounds(mobileMessagingCore.getStoredSessionBounds()).size());
        UserSessionEventBody userSessionEventBody = userSessionEventCaptor.getValue();
        assertEquals(sessionBounds, userSessionEventBody.getSessionBounds());
        assertEquals(CollectionUtils.setOf(DateTimeUtil.dateToISO8601UTCString(new Date(sessionStartMillis))), userSessionEventBody.getSessionStarts());
    }

    @Test
    public void shouldReportOneCustomEventOnServerWithValidationSync() throws Exception {
        CustomEvent customEvent = setupCustomEventWithProperties();

        userEventsSynchronizer.reportCustomEvent(customEvent, eventResultListener);

        assertNull(PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS));
        verify(mobileApiAppInstance, times(1)).sendUserCustomEvents(anyString(), eq(true), customEventCaptor.capture());
        verifyCustomEventRequestBody(customEvent);
        assertNull(PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS));
        verify(broadcaster, after(300).times(1)).customEventsReported();
    }

    @Test
    public void shouldPropagateValidationErrorOnReportingOneCustomEventOnServerSync() throws Exception {
        CustomEvent customEvent = setupCustomEventWithProperties();
        doThrow(new ApiIOException(ApiErrorCode.INVALID_VALUE, "Request is invalid"))
                .when(mobileApiAppInstance).sendUserCustomEvents(anyString(), anyBoolean(), any(UserCustomEventBody.class));

        userEventsSynchronizer.reportCustomEvent(customEvent, eventResultListener);

        assertNull(PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS));
        verify(mobileApiAppInstance, times(1)).sendUserCustomEvents(anyString(), eq(true), customEventCaptor.capture());
        verifyCustomEventRequestBody(customEvent);
        verify(broadcaster, after(300).times(1)).error(any(MobileMessagingError.class));
        assertNull(PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS));
    }

    @Test
    public void shouldReportOneCustomEventOnServerAsync() throws Exception {
        CustomEvent customEvent = setupCustomEventWithProperties();
        mobileMessagingCore.addUnreportedUserCustomEvent(customEvent);
        assertEquals(1, PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS).length);

        userEventsSynchronizer.reportCustomEvents();

        verify(mobileApiAppInstance, times(1)).sendUserCustomEvents(anyString(), eq(false), customEventCaptor.capture());
        verifyCustomEventRequestBody(customEvent);
        verify(broadcaster, after(300).times(1)).customEventsReported();
        assertNull(PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS));
    }

    @Test
    public void shouldReportMultipleCustomEventsOnServerAsync() throws Exception {
        CustomEvent customEvent = setupCustomEventWithProperties();
        CustomEvent customEvent2 = new CustomEvent();
        customEvent2.setProperty("yay", new CustomAttributeValue("kk"));
        customEvent2.setDefinitionId("9876");
        mobileMessagingCore.addUnreportedUserCustomEvent(customEvent);
        mobileMessagingCore.addUnreportedUserCustomEvent(customEvent2);
        assertEquals(2, PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS).length);

        userEventsSynchronizer.reportCustomEvents();

        verify(mobileApiAppInstance, times(1)).sendUserCustomEvents(anyString(), eq(false), customEventCaptor.capture());
        UserCustomEventBody userCustomEventBody = customEventCaptor.getValue();
        assertEquals(2, userCustomEventBody.getEvents().length);
        verify(broadcaster, after(300).times(1)).customEventsReported();
        assertNull(PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS));
    }

    @NonNull
    private CustomEvent setupCustomEventWithProperties() {
        CustomEvent customEvent = new CustomEvent();
        customEvent.setProperty("someBool", new CustomAttributeValue(true));
        customEvent.setProperty("someString", new CustomAttributeValue("string"));
        customEvent.setProperty("someDate", new CustomAttributeValue(new Date(date)));
        customEvent.setProperty("someNumber", new CustomAttributeValue(1.1));
        customEvent.setDefinitionId("12345");
        return customEvent;
    }

    private void verifyCustomEventRequestBody(CustomEvent customEvent) throws ParseException {
        UserCustomEventBody userCustomEventBody = customEventCaptor.getValue();
        assertEquals(customEvent.getDefinitionId(), userCustomEventBody.getEvents()[0].getDefinitionId());
        assertEquals(customEvent.getProperties().keySet(), userCustomEventBody.getEvents()[0].getProperties().keySet());
        assertEquals(customEvent.getProperties().get("someBool").booleanValue(), userCustomEventBody.getEvents()[0].getProperties().get("someBool"));
        assertEquals(customEvent.getProperties().get("someString").stringValue(), userCustomEventBody.getEvents()[0].getProperties().get("someString"));
        assertEquals(customEvent.getProperties().get("someNumber").numberValue(), userCustomEventBody.getEvents()[0].getProperties().get("someNumber"));

        Date someDateInUtc = DateTimeUtil.dateFromISO8601DateUTCString((String) userCustomEventBody.getEvents()[0].getProperties().get("someDate"));
        Date someDate = customEvent.getProperties().get("someDate").dateValue();

        int offsetUtcDate = TimeZone.getDefault().getOffset(someDateInUtc.getTime());
        int offsetLocalDate = TimeZone.getDefault().getOffset(someDate.getTime());

        assertEquals(someDate.getTime() - offsetLocalDate, someDateInUtc.getTime());
    }
}
