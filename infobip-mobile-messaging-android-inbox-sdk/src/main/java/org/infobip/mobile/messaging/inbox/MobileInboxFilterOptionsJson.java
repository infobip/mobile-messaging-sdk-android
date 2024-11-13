package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MobileInboxFilterOptionsJson {
    public static MobileInboxFilterOptions mobileInboxFilterOptionsFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        try {
            Date fromDateTime = null, toDateTime = null;
            String topic = null;
            Integer limit = null;

            if (json.has(MobileInboxFilterOptionsAttrs.fromDateTime) && !json.isNull(MobileInboxFilterOptionsAttrs.fromDateTime))
                fromDateTime = DateTimeUtil.ISO8601DateFromString(json.getString(MobileInboxFilterOptionsAttrs.fromDateTime));
            if (json.has(MobileInboxFilterOptionsAttrs.toDateTime) && !json.isNull(MobileInboxFilterOptionsAttrs.toDateTime))
                toDateTime = DateTimeUtil.ISO8601DateFromString(json.getString(MobileInboxFilterOptionsAttrs.toDateTime));
            if (json.has(MobileInboxFilterOptionsAttrs.topic) && !json.isNull(MobileInboxFilterOptionsAttrs.topic))
                topic = json.getString(MobileInboxFilterOptionsAttrs.topic);
            if (json.has(MobileInboxFilterOptionsAttrs.limit) && !json.isNull(MobileInboxFilterOptionsAttrs.limit))
                limit = json.getInt(MobileInboxFilterOptionsAttrs.limit);

            return new MobileInboxFilterOptions(
                    fromDateTime,
                    toDateTime,
                    topic,
                    limit
            );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}

