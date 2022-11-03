package org.infobip.mobile.messaging.inbox;

import android.os.Bundle;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.List;

/**
 * The class incapsulates user inbox data.
 */
public class Inbox {
    private static final JsonSerializer serializer = new JsonSerializer(false);

    private int countTotal;

    private int countUnread;

    private List<InboxMessage> messages;

    /**
     * Total number of messages available in the Inbox. Maximum is limited to 100 messages.
     */ 
    public int getCountTotal() {
        return countTotal;
    }

    public void setCountTotal(int countTotal) {
        this.countTotal = countTotal;
    }

    /**
     * Number of messages that not yet marked as seen/read. See `MobileMessaging.getInstance(context).setSeen(externalUserId:messageIds:object:)`.
     */
    public int getCountUnread() {
        return countUnread;
    }

    public void setCountUnread(int countUnread) {
        this.countUnread = countUnread;
    }

    /**
     * Array of inbox messages ordered by message send date-time.
     */
    public List<InboxMessage> getMessages() {
        return messages;
    }

    void setMessages(List<InboxMessage> messages) {
        this.messages = messages;
    }

    public static Inbox createFrom(Bundle bundle) {
        return InboxBundleMapper.inboxFromBundle(bundle);
    }

    @Override
    public String toString() {
        return serializer.serialize(this);
    }

    public Inbox fromString(String inboxDataString) {
        return serializer.deserialize(inboxDataString, Inbox.class);
    }
}
