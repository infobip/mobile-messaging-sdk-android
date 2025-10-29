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

    private InboxData inboxData;

    public static InboxMessage createFrom(Message message, InboxData inboxData) {
        return new InboxMessage(message.getMessageId(), message.getTitle(), message.getBody(), message.getSound(), message.isVibrate(), message.getIcon(),
                message.isSilent(), message.getCategory(), message.getFrom(), message.getReceivedTimestamp(), message.getSeenTimestamp(), message.getSentTimestamp(),
                message.getCustomPayload(), message.getInternalData(), message.getDestination(), message.getStatus(), message.getStatusMessage(), message.getContentUrl(),
                inboxData, message.getInAppStyle(), message.getInAppExpiryTimestamp(), message.getWebViewUrl(), message.getBrowserUrl(), message.getMessageType(), message.getDeeplink(),
                message.getInAppOpenTitle(), message.getInAppDismissTitle());
    }

    static Message toMessage(InboxMessage inboxMessage) {
        return new Message(inboxMessage.getMessageId(), inboxMessage.getTitle(), inboxMessage.getBody(), inboxMessage.getSound(), inboxMessage.isVibrate(), inboxMessage.getIcon(),
                inboxMessage.isSilent(), inboxMessage.getCategory(), inboxMessage.getFrom(), inboxMessage.getReceivedTimestamp(), inboxMessage.getSeenTimestamp(), inboxMessage.getSentTimestamp(),
                inboxMessage.getCustomPayload(), inboxMessage.getInternalData(), inboxMessage.getDestination(), inboxMessage.getStatus(), inboxMessage.getStatusMessage(), inboxMessage.getContentUrl(),
                inboxMessage.getInAppStyle(), inboxMessage.getInAppExpiryTimestamp(), inboxMessage.getWebViewUrl(), inboxMessage.getBrowserUrl(), inboxMessage.getMessageType(), inboxMessage.getDeeplink(),
                inboxMessage.getInAppOpenTitle(), inboxMessage.getInAppDismissTitle());
    }

    private InboxMessage(String messageId, String title, String body, String sound, boolean vibrate, String icon, boolean silent, String category,
                         String from, long receivedTimestamp, long seenTimestamp, long sentTimestamp, JSONObject customPayload, String internalData,
                         String destination, Status status, String statusMessage, String contentUrl, InboxData inboxData, InAppStyle inAppStyle, long expiryTime,
                         String webViewUrl, String browserUrl, String messageType, String deeplink, String inAppOpenTitle, String inAppDismissTitle) {
        super(messageId, title, body, sound, vibrate, icon, silent, category, from, receivedTimestamp, seenTimestamp, sentTimestamp, customPayload,
                internalData, destination, status, statusMessage, contentUrl, inAppStyle, expiryTime, webViewUrl, browserUrl, messageType, deeplink,
                inAppOpenTitle, inAppDismissTitle);
        this.inboxData = inboxData;
    }

    public String getTopic() { return getInboxData().getTopic(); }

    void setInboxData(InboxData inboxData) { this.inboxData = inboxData; }

    InboxData getInboxData() { return this.inboxData; }

    public boolean isSeen() { return getInboxData().isSeen(); }

    public void setSeen() {
        setInboxData(new InboxData(getInboxData().getTopic(), true));
    }
}
