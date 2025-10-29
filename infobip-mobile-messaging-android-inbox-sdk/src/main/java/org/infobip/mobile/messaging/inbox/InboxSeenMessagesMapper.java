/*
 * InboxSeenMessagesMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.api.inbox.InboxSeenMessages;
import org.infobip.mobile.messaging.mobileapi.seen.SeenMessagesMapper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class InboxSeenMessagesMapper extends InboxSeenMessages {

    static InboxSeenMessages fromMessageIds(String externalUserId, String[] messageIds) {
        List<Message> messages = new ArrayList<>();
        for (String seenMessage : messageIds) {
            String[] messageIdWithTimestamp = seenMessage.split(StringUtils.COMMA_WITH_SPACE);
            String messageId = messageIdWithTimestamp[0];
            String seenTimestampString = messageIdWithTimestamp[1];

            messages.add(new Message(messageId, SeenMessagesMapper.countDeltaInSeconds(seenTimestampString)));
        }
        return new InboxSeenMessages(externalUserId, messages.toArray(new Message[0]));
    }
}
