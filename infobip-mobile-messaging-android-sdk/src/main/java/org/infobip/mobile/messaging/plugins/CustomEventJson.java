package org.infobip.mobile.messaging.plugins;

import android.util.Log;

import com.google.gson.reflect.TypeToken;

import org.infobip.mobile.messaging.CustomAttributesMapper;
import org.infobip.mobile.messaging.CustomEvent;
import org.infobip.mobile.messaging.api.appinstance.UserCustomEventAtts;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * CustomEvent data mapper for JSON conversion in plugins
 */
public class CustomEventJson extends CustomEvent {
    public static CustomEvent fromJSON(JSONObject json) {
        CustomEvent customEvent = new CustomEvent();
        try {
            if (json.has(UserCustomEventAtts.definitionId)) {
                customEvent.setDefinitionId(json.optString(UserCustomEventAtts.definitionId));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error when serializing CustomEvent object:" + Log.getStackTraceString(e));
        }
        try {
            if (json.has(UserCustomEventAtts.properties)) {
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> properties = new JsonSerializer().deserialize(json.optString(UserCustomEventAtts.properties), type);
                customEvent.setProperties(CustomAttributesMapper.customAttsFromBackend(properties));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error when serializing CustomEvent object:" + Log.getStackTraceString(e));
        }
        return customEvent;
    }
}