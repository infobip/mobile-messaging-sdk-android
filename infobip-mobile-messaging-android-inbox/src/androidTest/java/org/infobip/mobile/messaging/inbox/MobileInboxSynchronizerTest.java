package org.infobip.mobile.messaging.inbox;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.inbox.MobileApiInbox;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Date;
import java.util.Collections;

public class MobileInboxSynchronizerTest extends MobileMessagingTestCase {

    private MobileInboxSynchronizer mobileInboxSynchronizer;
    private AndroidBroadcaster androidBroadcaster;
    private MobileApiInbox mobileApiInbox;
    private MobileMessaging.ResultListener<Inbox> inboxResultListener = mock(MobileMessaging.ResultListener.class);

    private String givenToken = "someToken";
    private String givenExternalUserId = "someExtUID";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        androidBroadcaster = mock(AndroidBroadcaster.class);
        mobileApiInbox = mock(MobileApiInbox.class);

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

        verify(mobileApiInbox, Mockito.after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, null, null);
    }

    @Test
    public void should_call_api_with_appcode() {
        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, null, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, Mockito.after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, null, null);
    }

    @Test
    public void should_call_api_with_only_required_filterOptions() {
        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(null, null, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, Mockito.after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, null, null, "sometopic", 15);
    }

    @Test
    public void should_call_api_with_filterOptions() {
        Date dateFrom = new Date(1640984400000L);
        Date dateTo = new Date(1654462800000L);

        MobileInboxFilterOptions filterOptions = new MobileInboxFilterOptions(dateFrom, dateTo, "sometopic", 15);

        mobileInboxSynchronizer.fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);

        String resultToken = "App " + mobileMessagingCore.getApplicationCode();

        verify(mobileApiInbox, Mockito.after(300).times(1)).fetchInbox(givenExternalUserId, resultToken, "1640984400000", "1654462800000", "sometopic", 15);
    }
}
