package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.api.seenstatus.SeenMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 25/04/16.
 */
public class SeenMessagesReport extends SeenMessages {

    public static org.infobip.mobile.messaging.api.seenstatus.SeenMessages fromMessageIds(String messageIds[]) {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < messageIds.length; i++) {
            messages.add(new Message(messageIds[i]));
        }
        return new SeenMessages(messages.toArray(new Message[messages.size()]));
    }
}
