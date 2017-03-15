package org.infobip.mobile.messaging.mobile.messages;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.api.messages.SyncMessagesResponse;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pandric
 * @since 09/09/16.
 */
class SyncMessagesResult extends UnsuccessfulResult {

    private List<Message> messages;

    SyncMessagesResult(Throwable exception) {
        super(exception);
    }

    SyncMessagesResult(SyncMessagesResponse syncMessagesResponse) {
        super(null);

        if (syncMessagesResponse == null) {
            return;
        }

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

            Message message = responseToMessage(messageResponse);
            this.messages.add(message);
        }
    }

    public List<Message> getMessages() {
        return messages;
    }

    private static Message responseToMessage(MessageResponse response) {
        JSONObject customPayload = null;
        try {
            customPayload = response.getCustomPayload() != null ? new JSONObject(response.getCustomPayload()) : null;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Geo geo = response.getInternalData() != null ?
                new JsonSerializer().deserialize(response.getInternalData(), Geo.class) : null;

        Message message = new Message(
                response.getMessageId(),
                response.getTitle(),
                response.getBody(),
                response.getSound(),
                "true".equals(response.getVibrate()),
                null,
                "true".equals(response.getSilent()),
                response.getCategory(),
                null,
                Time.now(),
                0,
                customPayload,
                geo,
                null,
                Message.Status.UNKNOWN,
                null
        );

        InternalDataMapper.updateMessageWithInternalData(message, response.getInternalData());
        return message;
    }
}
