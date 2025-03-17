package org.infobip.mobile.messaging.inbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.inbox.MobileApiInbox;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Date;
import java.util.Collections;

public class MobileInboxSynchronizerTest extends MobileMessagingTestCase {

    private MobileInboxSynchronizer mobileInboxSynchronizer;
    private AndroidBroadcaster androidBroadcaster;
    private MobileApiInbox mobileApiInbox;
    private MobileMessaging.ResultListener<Inbox> inboxResultListener = mock(MobileMessaging.ResultListener.class);
    private ArgumentCaptor<Inbox> dataCaptor;

    private String givenToken = "someToken";
    private String givenExternalUserId = "someExtUID";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        androidBroadcaster = mock(AndroidBroadcaster.class);
        mobileApiInbox = mock(MobileApiInbox.class);
        dataCaptor = forClass(Inbox.class);

        mobileInboxSynchronizer = new MobileInboxSynchronizer(
                context,
                mobileMessagingCore,
                androidBroadcaster,
                inboxBroadcaster,
                mobileApiInbox
        );

        given(mobileApiInbox.fetchInbox(any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(1, 1, Collections.singletonList(new MessageResponse())));
    }

    @Test
    public void should_call_api_with_bearer() {
        mobileInboxSynchronizer.fetchInbox(givenToken, givenExternalUserId, null, inboxResultListener);
        String resultToken = "Bearer " + givenToken;

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, null, null);
    }

    @Test
    public void should_call_api_with_appcode() {
        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, null, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, null, null);
    }

    @Ignore("Ignoring as part of MM-7095")
    @Test
    public void should_call_api_with_only_required_filterOptions() {
        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, "sometopic", 15);
    }

    @Ignore("Ignoring as part of MM-7095")
    @Test
    public void should_call_api_with_filterOptions() {
        Date dateFrom = new Date(1640984400000L);
        Date dateTo = new Date(1654462800000L);

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(dateFrom, dateTo, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, "1640984400000", "1654462800000", "sometopic", 15);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();
        assertEquals(1, returnedInbox.getMessages().size());
        assertEquals(1, returnedInbox.getCountUnread());
        assertEquals(1, returnedInbox.getCountTotal());
    }

    @Test
    public void should_call_api_and_work_with_null_messages_response() {
        given(mobileApiInbox.fetchInbox(any(), any(), any(), any(), any(), any()))
                .willReturn(new FetchInboxResponse(0, 0, null));

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        verify(inboxBroadcaster, after(300).atLeastOnce()).inboxFetched(dataCaptor.capture());
        Inbox returnedInbox = dataCaptor.getValue();
        assertNull(returnedInbox.getMessages());
    }
}
