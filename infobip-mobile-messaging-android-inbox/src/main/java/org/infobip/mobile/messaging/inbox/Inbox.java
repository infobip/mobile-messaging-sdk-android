package org.infobip.mobile.messaging.inbox;

import android.os.Bundle;

import java.util.List;

public class Inbox {
    private int countTotal;

    private int countUnread;

    private List<InboxMessage> messages;

    public int getCountTotal() {
        return countTotal;
    }

    public void setCountTotal(int countTotal) {
        this.countTotal = countTotal;
    }

    public int getCountUnread() {
        return countUnread;
    }

    public void setCountUnread(int countUnread) {
        this.countUnread = countUnread;
    }

    public List<InboxMessage> getMessages() {
        return messages;
    }

    void setMessages(List<InboxMessage> messages) {
        this.messages = messages;
    }

    public static Inbox createFrom(Bundle bundle) {
        return InboxBundleMapper.inboxFromBundle(bundle);
    }
}
