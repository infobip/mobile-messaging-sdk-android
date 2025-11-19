/*
 * InboxDataMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import androidx.annotation.Nullable;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

/**
 * Used to parse Inbox stringified json from internalData
 */
public final class InboxDataMapper {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    private static class Inbox {
        String topic;

        boolean seen;

        public Inbox(String topic, boolean seen) {
            this.topic = topic;
            this.seen = seen;
        }
    }

    /**
     * Serializes inbox object to json data
     *
     * @param topic - message topic to serialize
     * @param seen  - message seen status to serialize
     * @return String - Inbox as json string
     */
    @Nullable
    public static String inboxDataToInternalData(String topic, boolean seen) {
        return topic != null ? serializer.serialize(new Inbox(topic, seen)) : null;
    }

    /**
     * Returns inbox topic from internal data
     *
     * @param internalDataJson internalDataJson to deserialize
     * @return topic string
     */
    @Nullable
    public static String inboxTopicFromInternalData(String internalDataJson) {
        return internalDataJson != null ? serializer.deserialize(internalDataJson, Inbox.class).topic : null;
    }

    /**
     * Returns inbox seen from internal data
     *
     * @param internalDataJson internalDataJson to deserialize
     * @return seen boolean
     */
    public static boolean inboxSeenFromInternalData(String internalDataJson) {
        return internalDataJson != null && serializer.deserialize(internalDataJson, Inbox.class).seen;
    }
}
