package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.dal.json.InternalDataMapper;

public class InboxData extends InternalDataMapper.InternalData {

    private Inbox inbox;

    public InboxData(Inbox inbox) { this.inbox = inbox; }

    String getTopic() { return getInbox().getTopic(); }

    boolean isSeen() { return getInbox().isSeen(); }

    public Inbox getInbox() { return inbox; }

    static class Inbox {
        private final String topic;
        private final boolean seen;

        public Inbox(String topic, boolean seen) {
            this.topic = topic;
            this.seen = seen;
        }

        public String getTopic() { return this.topic; }

        public boolean isSeen() { return this.seen; }
    }
}
