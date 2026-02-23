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
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;

/**
 * Used to parse Inbox stringified json from internalData
 */
public final class InboxDataMapper {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    static class InboxData extends InternalDataMapper.InternalData {
        private final Inbox inbox;

        public InboxData(Inbox inbox) {
            this.inbox = inbox;
        }

        public InboxData(String topic, boolean isSeen) {
            this.inbox = new Inbox(topic, isSeen);
        }

        public String getTopic() {
            Inbox inbox = getInbox();
            return inbox != null ? inbox.getTopic() : null;
        }

        public boolean isSeen() {
            Inbox inbox = getInbox();
            return inbox != null && inbox.isSeen();
        }

        protected Inbox getInbox() {
            return inbox;
        }

        static class Inbox {
            private final String topic;
            private final boolean seen;

            public Inbox(String topic, boolean seen) {
                this.topic = topic;
                this.seen = seen;
            }

            public String getTopic() {
                return this.topic;
            }

            public boolean isSeen() {
                return this.seen;
            }
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
        return topic != null ? serializer.serialize(new InboxData(topic, seen)) : null;
    }

    /**
     * Returns inbox topic from internal data
     *
     * @param internalDataJson internalDataJson to deserialize
     * @return topic string
     */
    @Nullable
    public static String inboxTopicFromInternalData(String internalDataJson) {
        try {
            return internalDataJson != null ? serializer.deserialize(internalDataJson, InboxData.class).getTopic() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns inbox seen from internal data
     *
     * @param internalDataJson internalDataJson to deserialize
     * @return seen boolean
     */
    public static boolean inboxSeenFromInternalData(String internalDataJson) {
        try {
            return internalDataJson != null && serializer.deserialize(internalDataJson, InboxData.class).isSeen();
        } catch (Exception e) {
            return false;
        }
    }
}
