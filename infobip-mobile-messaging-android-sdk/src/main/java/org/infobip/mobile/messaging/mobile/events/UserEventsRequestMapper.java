package org.infobip.mobile.messaging.mobile.events;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.UserMapper;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserCustomEventBody;
import org.infobip.mobile.messaging.api.appinstance.UserSessionEventBody;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserEventsRequestMapper {

    private static final JsonSerializer nullSerializer = new JsonSerializer(true);

    public static String toJson(UserCustomEventBody.CustomEvent customEvent) {
        return nullSerializer.serialize(customEvent);
    }

    public static UserCustomEventBody.CustomEvent fromJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return nullSerializer.deserialize(json, UserCustomEventBody.CustomEvent.class);
    }

    public static UserCustomEventBody createCustomEventRequest(CustomEvent customEvent) {
        Map<String, Object> payload = null;
        if (StringUtils.isBlank(customEvent.getDefinitionId())) {
            return null;
        }
        if (customEvent.getProperties() != null) {
            payload = UserMapper.customAttsToBackend(customEvent.getProperties());
        }
        UserCustomEventBody.CustomEvent customEventRequestBody = new UserCustomEventBody.CustomEvent(
                customEvent.getDefinitionId(),
                DateTimeUtil.ISO8601DateUTCToString(Time.date()),
                payload
        );
        UserCustomEventBody.CustomEvent[] customEvents = {customEventRequestBody};
        return new UserCustomEventBody(customEvents);
    }

    public static UserSessionEventBody createUserSessionEventRequest(long sessionStartsMillis, String[] storedSessionBounds, AppInstance systemData) {
        HashMap<String, String> sessionBounds = getSessionBounds(storedSessionBounds);
        if (sessionBounds.size() == 0 || systemData == null) {
            return null;
        }

        Set<String> sessionStarts = getSessionStarts(sessionStartsMillis);

        return new UserSessionEventBody(systemData, sessionStarts, sessionBounds);
    }

    @NonNull
    static HashMap<String, String> getSessionBounds(String[] storedSessionBounds) {
        HashMap<String, String> sessionBounds = new HashMap<>();
        if (storedSessionBounds == null || storedSessionBounds.length == 0) {
            return sessionBounds;
        }

        for (String sessionBound : storedSessionBounds) {
            String[] splitSession = sessionBound.split(UserSessionTracker.SESSION_BOUNDS_DELIMITER);
            String sessionStart = splitSession[0];
            String sessionEnd = splitSession[1];
            sessionBounds.put(sessionStart, sessionEnd);
        }
        return sessionBounds;
    }

    @NonNull
    static Set<String> getSessionStarts(long sessionStartsMillis) {
        Set<String> sessionStarts = new HashSet<>();
        if (sessionStartsMillis != 0) {
            String sessionStartsDate = DateTimeUtil.ISO8601DateUTCToString(new Date(sessionStartsMillis));
            sessionStarts = CollectionUtils.setOf(sessionStartsDate);
        }
        return sessionStarts;
    }
}
