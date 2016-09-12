package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.messages.v3.MessageResponse;
import org.infobip.mobile.messaging.api.messages.v3.SyncMessagesResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class SyncMessagesResult extends UnsuccessfulResult {

    private List<Message> messages;

    public SyncMessagesResult(Throwable exception) {
        super(exception);
    }

    public SyncMessagesResult(SyncMessagesResponse syncMessagesResponse) {
        super(null);
        List<MessageResponse> payloads = syncMessagesResponse.getPayloads();
        mapResponseToMessage(payloads);
    }

    private void mapResponseToMessage(List<MessageResponse> payloads) {
        this.messages = new ArrayList<>(payloads.size());
        for (MessageResponse messageResponse : payloads) {
            Message message = new Message();
            message.setMessageId(messageResponse.getMessageId());
            message.setBody(messageResponse.getBody());

            this.messages.add(message);
        }
    }

    public List<Message> getMessages() {
        return messages;
    }
}
