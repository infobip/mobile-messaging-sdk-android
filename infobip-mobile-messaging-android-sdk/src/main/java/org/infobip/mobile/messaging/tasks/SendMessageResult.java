package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.api.messages.MoMessageDelivery;

/**
 * @author sslavin
 * @since 21/07/16.
 */
public class SendMessageResult extends UnsuccessfulResult {

    MoMessageDelivery messages[];

    public SendMessageResult(Throwable exception) {
        super(exception);
    }

    public SendMessageResult(MoMessageDelivery messages[]) {
        super(null);
        this.messages = messages;
    }

    public MoMessageDelivery[] getMessageDeliveries() {
        return messages;
    }

    public void setMessages(MoMessageDelivery[] messages) {
        this.messages = messages;
    }
}
