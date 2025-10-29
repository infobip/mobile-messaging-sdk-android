/*
 * MoMessageMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.messages;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.messages.MoMessage;
import org.infobip.mobile.messaging.api.messages.MoMessageDelivery;
import org.infobip.mobile.messaging.api.messages.MoMessagesBody;
import org.infobip.mobile.messaging.api.messages.MoMessagesResponse;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 21/08/2017.
 */

public class MoMessageMapper {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    static Message[] messages(MoMessagesResponse response) {

        if (response == null || response.getMessages() == null || response.getMessages().length == 0) {
            return new Message[0];
        }

        List<Message> messages = new ArrayList<>(response.getMessages().length);
        for (MoMessageDelivery delivery : response.getMessages()) {
            Message message = new Message();
            message.setMessageId(delivery.getMessageId());
            message.setDestination(delivery.getDestination());
            message.setBody(delivery.getText());
            message.setStatusMessage(delivery.getStatus());
            message.setReceivedTimestamp(Time.now());
            message.setSeenTimestamp(Time.now());
            message.setCustomPayload(delivery.getCustomPayload() != null ? new JSONObject(delivery.getCustomPayload()) : null);
            Message.Status status = Message.Status.UNKNOWN;
            int statusCode = delivery.getStatusCode();
            if (statusCode < Message.Status.values().length) {
                status = Message.Status.values()[statusCode];
            } else {
                MobileMessagingLogger.w(delivery.getMessageId() + ":Unexpected status code: " + statusCode);
            }
            message.setStatus(status);
            messages.add(message);
        }

        return messages.toArray(new Message[0]);
    }

    static MoMessagesBody body(String pushRegistrationId, Message[] messages) {
        List<MoMessage> moMessages = new ArrayList<>();
        for (Message message : messages) {
            String customPayloadString = message.getCustomPayload() != null ? message.getCustomPayload().toString() : null;
            Map customPayloadMap = serializer.deserialize(customPayloadString, Map.class);
            String internalData = message.getInternalData();
            moMessages.add(new MoMessage(message.getMessageId(), message.getDestination(), message.getBody(), InternalDataMapper.getInternalDataInitialMessageId(internalData), InternalDataMapper.getInternalDataBulkId(internalData), customPayloadMap));
        }

        MoMessagesBody moMessagesBody = new MoMessagesBody();
        moMessagesBody.setFrom(pushRegistrationId);
        moMessagesBody.setMessages(moMessages.toArray(new MoMessage[0]));

        return moMessagesBody;
    }
}
