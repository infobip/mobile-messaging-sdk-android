package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.api.shaded.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * @auhtor sslavin
 * @since 25/04/16.
 */
public class SeenMessages {

    private SeenMessage messages[];

    private SeenMessages() {
    }

    public static SeenMessages withMessageIds(String messageIds[]) {
        SeenMessages seenMessages = new SeenMessages();
        seenMessages.messages = new SeenMessage[messageIds.length];
        for (int i = 0; i < messageIds.length; i++) {
            seenMessages.messages[i] = new SeenMessage(messageIds[i]);
        }
        return seenMessages;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
