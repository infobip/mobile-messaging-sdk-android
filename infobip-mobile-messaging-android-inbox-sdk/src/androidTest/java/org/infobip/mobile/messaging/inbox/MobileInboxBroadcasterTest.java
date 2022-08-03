package org.infobip.mobile.messaging.inbox;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

public class MobileInboxBroadcasterTest extends MobileMessagingTestCase {

    private MobileInboxBroadcaster mobileInboxBroadcaster;
    private ArgumentCaptor<Intent> intentArgumentCaptor;

    private String givenMessageId = "someMessageId";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mobileInboxBroadcaster = new MobileInboxBroadcasterImpl(contextMock);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    }

    @Test
    public void should_send_inbox_fetched_event() {
        // Given
        Inbox inbox = createInbox(1, 1, Collections.singletonList(createMessage(givenMessageId)));

        // When
        mobileInboxBroadcaster.inboxFetched(inbox);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(MobileInboxEvent.INBOX_MESSAGES_FETCHED.getKey(), intent.getAction());

        Inbox result = Inbox.createFrom(intent.getExtras());
        assertEquals(1, result.getCountTotal());
        assertEquals(1, result.getCountUnread());
        assertEquals(givenMessageId, result.getMessages().get(0).getMessageId());
    }

    @Test
    public void should_send_seen_reports_event() {
        // When
        mobileInboxBroadcaster.seenReported(givenMessageId);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(MobileInboxEvent.INBOX_SEEN_REPORTED.getKey(), intent.getAction());

        String someId = intent.getExtras().toString();
        assertEquals(givenMessageId, intent.getStringArrayExtra(BroadcastParameter.EXTRA_INBOX_SEEN_IDS)[0]);
    }
}
