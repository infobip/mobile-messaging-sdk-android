package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InboxMapper {
    public static Inbox fromBackend(FetchInboxResponse fetchInboxResponse) {
        Inbox inbox = new Inbox();
        inbox.setCountTotal(fetchInboxResponse.getCountTotal());
        inbox.setCountUnread(fetchInboxResponse.getCountUnread());
        if (inbox.getMessages() != null) {
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
            e.printStackTrace();
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
}
