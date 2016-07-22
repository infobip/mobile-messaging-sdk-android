package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.api.messages.MoOutgoingMessageDelivery;

/**
 * @author sslavin
 * @since 21/07/16.
 */
public class SendMessageResult extends UnsuccessfulResult {

    MoOutgoingMessageDelivery messages[];

    public SendMessageResult(Throwable exception) {
        super(exception);
    }

    public SendMessageResult(MoOutgoingMessageDelivery messages[]) {
        super(null);
        this.messages = messages;
    }

    public MoOutgoingMessageDelivery[] getMessageDeliveries() {
        return messages;
    }

    public void setMessages(MoOutgoingMessageDelivery[] messages) {
        this.messages = messages;
    }
}
