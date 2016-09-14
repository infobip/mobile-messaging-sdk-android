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

            Message message = SyncMessageDelivery.toMessage(messageResponse);
            this.messages.add(message);
        }
    }

    public List<Message> getMessages() {
        return messages;
    }

    static class SyncMessageDelivery extends Message {

        static Message toMessage(MessageResponse messageResponse) {
            Bundle bundle = new Bundle();
            bundle.putString(BundleField.MESSAGE_ID.getKey(), messageResponse.getMessageId());
            bundle.putString(BundleField.TITLE.getKey(), messageResponse.getTitle());
            bundle.putString(BundleField.BODY.getKey(), messageResponse.getBody());
            bundle.putString(BundleField.SOUND.getKey(), messageResponse.getSound());
            bundle.putString(BundleField.VIBRATE.getKey(), messageResponse.getVibrate());
            bundle.putString(BundleField.SILENT.getKey(), messageResponse.getSilent());
            bundle.putString(BundleField.CUSTOM_PAYLOAD.getKey(), messageResponse.getCustomPayload());
            bundle.putString(BundleField.INTERNAL_DATA.getKey(), messageResponse.getInternalData());

            return Message.createFrom(bundle);
        }
    }
}
