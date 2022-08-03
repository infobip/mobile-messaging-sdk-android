package org.infobip.mobile.messaging.inbox;

import androidx.annotation.Nullable;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

/**
 * Used to parse Inbox stringified json from internalData
 */
public final class InboxDataMapper {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    /**
     * Serializes json data to InboxData object
     *
     * @param internalDataJson inboxData to deserialize
     * @return InboxData object from json data
     */
    @Nullable
    public static InboxData inboxDataFromInternalData(String internalDataJson) {
        return internalDataJson != null ? serializer.deserialize(internalDataJson, InboxData.class) : null;
    }

    /**
     * Serializes InboxData object to json data
     *
     * @param inbox inboxData object to serialize
     * @return String - inboxData as json string
     */
    @Nullable
    public static String inboxDataToInternalData(InboxData inbox) {
        return inbox != null ? serializer.serialize(inbox) : null;
    }
}
