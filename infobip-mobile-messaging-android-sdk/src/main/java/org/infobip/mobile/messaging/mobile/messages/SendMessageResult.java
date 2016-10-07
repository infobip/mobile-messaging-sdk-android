package org.infobip.mobile.messaging.mobile.messages;

import org.infobip.mobile.messaging.api.messages.MoMessageDelivery;
import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

/**
 * @author sslavin
 * @since 21/07/16.
 */
class SendMessageResult extends UnsuccessfulResult {

    private MoMessageDelivery messages[];

    SendMessageResult(Throwable exception) {
        super(exception);
    }

    SendMessageResult(MoMessageDelivery messages[]) {
        super(null);
        this.messages = messages;
    }

    MoMessageDelivery[] getMessageDeliveries() {
        return messages;
    }

    public void setMessages(MoMessageDelivery[] messages) {
        this.messages = messages;
    }
}
