package org.infobip.mobile.messaging.inbox;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InboxMapper {
    public static Inbox fromBackend(@NonNull FetchInboxResponse fetchInboxResponse) {
        Inbox inbox = new Inbox();
        inbox.setCountTotal(fetchInboxResponse.getCountTotal());
        inbox.setCountUnread(fetchInboxResponse.getCountUnread());
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
            MobileMessagingLogger.w("Cannot get Inbox response", e);
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
                InboxDataMapper.inboxDataFromInternalData(internalData));

        InternalDataMapper.updateMessageWithInternalData(message, internalData);
        return message;
    }

    public static JSONObject toJSON(final Inbox inbox) {
        if (inbox == null) {
            return new JSONObject();
        }
        try {
            return new JSONObject(inbox.toString());
        } catch (Exception e) {
            MobileMessagingLogger.w("Cannot convert Inbox toJSON", e);
            return new JSONObject();
        }
    }
}