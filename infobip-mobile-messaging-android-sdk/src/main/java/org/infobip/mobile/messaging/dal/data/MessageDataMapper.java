/*
 * MessageDataMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.dal.data;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.json.JSONArrayAdapter;
import org.infobip.mobile.messaging.dal.json.JSONObjectAdapter;

import androidx.annotation.NonNull;

public class MessageDataMapper {

    protected static final JsonSerializer serializer = new JsonSerializer(false, new JSONObjectAdapter(), new JSONArrayAdapter());

    public static Message messageFromString(@NonNull String str) {
        return serializer.deserialize(str, Message.class);
    }

    public static String messageToString(@NonNull Message message) {
        return serializer.serialize(message);
    }
}
