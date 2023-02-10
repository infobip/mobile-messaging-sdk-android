package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.dal.json.InternalDataMapper;

public class InboxData extends InternalDataMapper.InternalData {

    private Inbox inbox;

    public InboxData(Inbox inbox) { this.inbox = inbox; }

    public InboxData(String topic, boolean isSeen) {
        this.inbox = new Inbox(topic, isSeen);
    }

    public String getTopic() { return getInbox().getTopic(); }

    public boolean isSeen() { return getInbox().isSeen(); }

    protected Inbox getInbox() { return inbox; }

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
