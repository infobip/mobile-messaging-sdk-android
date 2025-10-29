/*
 * SeenMessagesMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.seen;

import org.infobip.mobile.messaging.api.messages.SeenMessages;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 25/04/16.
 */
public class SeenMessagesMapper extends SeenMessages {

    static SeenMessages fromMessageIds(String[] messageIds) {
        List<Message> messages = new ArrayList<>();
        for (String seenMessage : messageIds) {
            String[] messageIdWithTimestamp = seenMessage.split(StringUtils.COMMA_WITH_SPACE);
            String messageId = messageIdWithTimestamp[0];
            String seenTimestampString = messageIdWithTimestamp[1];

            messages.add(new Message(messageId, countDeltaInSeconds(seenTimestampString)));
        }
        return new SeenMessages(messages.toArray(new Message[0]));
    }

    public static long countDeltaInSeconds(String seenTimestampString) {
        long seenTimestamp = Long.valueOf(seenTimestampString);
        long deltaTimestamp = Time.now() - seenTimestamp;
        return Math.round((float) deltaTimestamp / 1000);
    }
}
