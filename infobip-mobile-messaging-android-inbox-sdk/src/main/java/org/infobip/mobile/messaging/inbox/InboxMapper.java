/*
 * InboxMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InboxMapper {
    public static Inbox fromBackend(@NonNull FetchInboxResponse fetchInboxResponse) {
        Inbox inbox = new Inbox();
        inbox.setCountTotal(fetchInboxResponse.getCountTotal());
        inbox.setCountUnread(fetchInboxResponse.getCountUnread());
        inbox.setCountTotalFiltered(fetchInboxResponse.getCountTotalFiltered());
        inbox.setCountUnreadFiltered(fetchInboxResponse.getCountUnreadFiltered());
        if (fetchInboxResponse.getMessages() != null) {
            List<InboxMessage> inboxMessages = new ArrayList<>(fetchInboxResponse.getMessages().size());
            for (MessageResponse messageResponse : fetchInboxResponse.getMessages()) {
                inboxMessages.add(responseToMessage(messageResponse));
            }
            inbox.setMessages(inboxMessages);
        }
        return inbox;
    }

    private static InboxMessage responseToMessage(MessageResponse response) {
        JSONObject customPayload = null;
        try {
            customPayload = response.getCustomPayload() != null ? new JSONObject(response.getCustomPayload()) : null;
        } catch (JSONException e) {
            MobileMessagingLogger.e("Cannot get Inbox response", e);
        }

        final String internalData = response.getInternalData();
        InboxMessage message = InboxMessage.createFrom(new Message(
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
                        InternalDataMapper.getInternalDataBrowserUrl(internalData),
                        InternalDataMapper.getInternalDataMessageType(internalData),
                        InternalDataMapper.getInternalDataDeeplinkUri(internalData),
                        InternalDataMapper.getInternalDataInAppOpenTitle(internalData),
                        InternalDataMapper.getInternalDataInAppDismissTitle(internalData)),
                InboxDataMapper.inboxTopicFromInternalData(internalData),
                InboxDataMapper.inboxSeenFromInternalData(internalData)
        );

        InternalDataMapper.updateMessageWithInternalData(message, internalData);
        return message;
    }

    public static JSONObject toJSON(final Inbox inbox) {
        if (inbox == null) {
            return new JSONObject();
        }
        try {
            JSONObject jsonObject = new JSONObject().put("countTotal", inbox.getCountTotal())
                    .put("countUnread", inbox.getCountUnread())
                    .putOpt("countTotalFiltered", inbox.getCountTotalFiltered())
                    .putOpt("countUnreadFiltered", inbox.getCountUnreadFiltered());
            JSONArray jsonArray = new JSONArray();
            for (InboxMessage message : inbox.getMessages()) {
                jsonArray.put(toJSON(message));
            }
            jsonObject.putOpt("messages", jsonArray);
            return jsonObject;
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot convert Inbox toJSON", e);
            return new JSONObject();
        }
    }

    private static JSONObject toJSON(final InboxMessage message) {
        try {
            return new JSONObject()
                    .putOpt("messageId", message.getMessageId())
                    .putOpt("title", message.getTitle())
                    .putOpt("body", message.getBody())
                    .putOpt("sound", message.getSound())
                    .putOpt("vibrate", message.isVibrate())
                    .putOpt("icon", message.getIcon())
                    .putOpt("silent", message.isSilent())
                    .putOpt("category", message.getCategory())
                    .putOpt("from", message.getFrom())
                    .putOpt("receivedTimestamp", message.getReceivedTimestamp())
                    .putOpt("customPayload", message.getCustomPayload())
                    .putOpt("contentUrl", message.getContentUrl())
                    .putOpt("seen", message.getSeenTimestamp() != 0)
                    .putOpt("seenDate", message.getSeenTimestamp())
                    .putOpt("chat", message.isChatMessage())
                    .putOpt("browserUrl", message.getBrowserUrl())
                    .putOpt("webViewUrl", message.getWebViewUrl())
                    .putOpt("deeplink", message.getDeeplink())
                    .putOpt("inAppOpenTitle", message.getInAppOpenTitle())
                    .putOpt("inAppDismissTitle", message.getInAppDismissTitle())
                    .putOpt("topic", message.getTopic())
                    .putOpt("seen", message.isSeen());
        } catch (JSONException e) {
            MobileMessagingLogger.e("Cannot convert message to JSON: ", e);
            return null;
        }
    }
}