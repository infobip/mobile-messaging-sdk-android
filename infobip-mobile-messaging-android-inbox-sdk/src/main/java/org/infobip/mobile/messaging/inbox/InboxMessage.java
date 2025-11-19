/*
 * InboxMessage.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.Message;
import org.json.JSONObject;

public class InboxMessage extends Message {
    private String topic;
    private boolean seen;

    public static InboxMessage createFrom(Message message, String topic, boolean seen) {
        return new InboxMessage(message.getMessageId(), message.getTitle(), message.getBody(), message.getSound(), message.isVibrate(), message.getIcon(),
                message.isSilent(), message.getCategory(), message.getFrom(), message.getReceivedTimestamp(), message.getSeenTimestamp(), message.getSentTimestamp(),
                message.getCustomPayload(), message.getInternalData(), message.getDestination(), message.getStatus(), message.getStatusMessage(), message.getContentUrl(),
                topic, seen, message.getInAppStyle(), message.getInAppExpiryTimestamp(), message.getWebViewUrl(), message.getBrowserUrl(), message.getMessageType(), message.getDeeplink(),
                message.getInAppOpenTitle(), message.getInAppDismissTitle());
    }

    static Message toMessage(InboxMessage inboxMessage) {
        return new Message(inboxMessage.getMessageId(), inboxMessage.getTitle(), inboxMessage.getBody(), inboxMessage.getSound(), inboxMessage.isVibrate(), inboxMessage.getIcon(),
                inboxMessage.isSilent(), inboxMessage.getCategory(), inboxMessage.getFrom(), inboxMessage.getReceivedTimestamp(), inboxMessage.getSeenTimestamp(), inboxMessage.getSentTimestamp(),
                inboxMessage.getCustomPayload(), inboxMessage.getInternalData(), inboxMessage.getDestination(), inboxMessage.getStatus(), inboxMessage.getStatusMessage(), inboxMessage.getContentUrl(),
                inboxMessage.getInAppStyle(), inboxMessage.getInAppExpiryTimestamp(), inboxMessage.getWebViewUrl(), inboxMessage.getBrowserUrl(), inboxMessage.getMessageType(), inboxMessage.getDeeplink(),
                inboxMessage.getInAppOpenTitle(), inboxMessage.getInAppDismissTitle());
    }

    InboxMessage(String messageId, String title, String body, String sound, boolean vibrate, String icon, boolean silent, String category,
                 String from, long receivedTimestamp, long seenTimestamp, long sentTimestamp, JSONObject customPayload, String internalData,
                 String destination, Status status, String statusMessage, String contentUrl, String topic, boolean seen, InAppStyle inAppStyle, long expiryTime,
                 String webViewUrl, String browserUrl, String messageType, String deeplink, String inAppOpenTitle, String inAppDismissTitle) {
        super(messageId, title, body, sound, vibrate, icon, silent, category, from, receivedTimestamp, seenTimestamp, sentTimestamp, customPayload,
                internalData, destination, status, statusMessage, contentUrl, inAppStyle, expiryTime, webViewUrl, browserUrl, messageType, deeplink,
                inAppOpenTitle, inAppDismissTitle);
        this.topic = topic;
        this.seen = seen;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen() {
        this.seen = true;
    }
}
