package org.infobip.mobile.messaging.mobile.seen;

import org.infobip.mobile.messaging.api.messages.SeenMessages;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 25/04/16.
 */
class SeenMessagesMapper extends SeenMessages {

    static SeenMessages fromMessageIds(String messageIds[]) {
        List<Message> messages = new ArrayList<>();
        for (String seenMessage : messageIds) {
            String[] messageIdWithTimestamp = seenMessage.split(StringUtils.COMMA_WITH_SPACE);
            String messageId = messageIdWithTimestamp[0];
            String seenTimestampString = messageIdWithTimestamp[1];

            long seenTimestamp = Long.valueOf(seenTimestampString);
            long deltaTimestamp = Time.now() - seenTimestamp;
            long deltaInSeconds = Math.round((float) deltaTimestamp / 1000);

            messages.add(new Message(messageId, deltaInSeconds));
        }
        return new SeenMessages(messages.toArray(new Message[messages.size()]));
    }
}
