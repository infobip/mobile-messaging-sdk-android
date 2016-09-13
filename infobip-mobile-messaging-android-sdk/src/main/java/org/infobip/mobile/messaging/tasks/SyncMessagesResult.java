package org.infobip.mobile.messaging.tasks;

import android.os.Bundle;

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
        if (payloads == null) return;

        this.messages = new ArrayList<>(payloads.size());
        for (MessageResponse messageResponse : payloads) {
            if (messageResponse == null) {
                continue;
            }

            Bundle bundle = new Bundle();
            bundle.putString("gcm.notification.messageId", messageResponse.getMessageId());
            bundle.putString("gcm.notification.title", messageResponse.getTitle());
            bundle.putString("gcm.notification.body", messageResponse.getBody());
            bundle.putString("gcm.notification.sound", messageResponse.getSound());
            bundle.putString("gcm.notification.vibrate", messageResponse.getVibrate());
            bundle.putString("gcm.notification.silent", messageResponse.getSilent());
            bundle.putString("customPayload", messageResponse.getCustomPayload());
            bundle.putString("internalData", messageResponse.getInternalData());

            Message message = Message.createFrom(bundle);
            this.messages.add(message);
        }
    }

    public List<Message> getMessages() {
        return messages;
    }
}
