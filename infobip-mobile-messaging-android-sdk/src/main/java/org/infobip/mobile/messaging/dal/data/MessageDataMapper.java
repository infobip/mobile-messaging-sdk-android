package org.infobip.mobile.messaging.dal.data;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.json.JSONArrayAdapter;
import org.infobip.mobile.messaging.dal.json.JSONObjectAdapter;

public class MessageDataMapper {

    protected static final JsonSerializer serializer = new JsonSerializer(false, new JSONObjectAdapter(), new JSONArrayAdapter());

    public static Message messageFromString(@NonNull String str) {
        return serializer.deserialize(str, Message.class);
    }

    public static String messageToString(@NonNull Message message) {
        return serializer.serialize(message);
    }
}
