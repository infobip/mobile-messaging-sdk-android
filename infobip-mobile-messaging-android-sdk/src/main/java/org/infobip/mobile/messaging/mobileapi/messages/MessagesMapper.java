package org.infobip.mobile.messaging.mobileapi.messages;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author pandric
 * @since 09/09/16.
 */
class MessagesMapper {

    static List<Message> mapResponseToMessages(List<MessageResponse> payloads) {
        if (payloads == null) return Collections.emptyList();

        List<Message> messages = new ArrayList<>(payloads.size());
        for (MessageResponse payload : payloads) {
            if (payload == null) {
                continue;
            }

            Message message = responseToMessage(payload);
            messages.add(message);
        }
        return messages;
    }


    private static Message responseToMessage(MessageResponse response) {
        JSONObject customPayload = null;
        try {
            customPayload = response.getCustomPayload() != null ? new JSONObject(response.getCustomPayload()) : null;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String internalData = response.getInternalData();
        Message message = new Message(
                response.getMessageId(),
                response.getTitle(),
                response.getBody(),
                response.getSound(),
                !"false".equals(response.getVibrate()),
                null,
                "true".equals(response.getSilent()),
                response.getCategory(),
                null,
                Time.now(),
                0,
                InternalDataMapper.getInternalDataSendDateTime(internalData),
                customPayload,
                internalData,
                null,
                Message.Status.UNKNOWN,
                null,
                InternalDataMapper.getInternalDataContentUrl(internalData),
                InternalDataMapper.getInternalDataInAppStyle(internalData),
                InternalDataMapper.getInternalDataInAppExpiryDateTime(internalData),
                InternalDataMapper.getInternalDataWebViewUrl(internalData),
                InternalDataMapper.getInternalDataMessageType(internalData),
                InternalDataMapper.getInternalDataDeeplinkUri(internalData)
        );

        InternalDataMapper.updateMessageWithInternalData(message, internalData);
        return message;
    }
}
