package org.infobip.mobile.messaging.inbox;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.junit.Test;
import org.mockito.Mockito;

public class MobileInboxTest extends MobileMessagingTestCase {

    private MobileInboxSynchronizer mobileInboxSynchronizer;
    private InboxSeenStatusReporter inboxSeenStatusReporter;
    private AndroidBroadcaster androidBroadcaster;
    private MobileInboxImpl mobileInboxImpl;
    private MobileMessaging.ResultListener<Inbox> inboxResultListener = mock(MobileMessaging.ResultListener.class);
    private MobileMessaging.ResultListener<String[]> seenResultListener = mock(MobileMessaging.ResultListener.class);


    private String givenToken = "someToken";
    private String givenExternalUserId = "someExtUID";
    private String givenTopic = "someTopic";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        androidBroadcaster = mock(AndroidBroadcaster.class);
        mobileInboxSynchronizer = mock(MobileInboxSynchronizer.class);
        inboxSeenStatusReporter = mock(InboxSeenStatusReporter.class);

        mobileInboxImpl = new MobileInboxImpl(context,
                androidBroadcaster,
                inboxBroadcaster,
                mobileApiResourceProvider,
                mobileInboxSynchronizer,
                inboxSeenStatusReporter
        );
    }

    @Test
    public void fetchInbox_should_be_called_with_null_token() {
        MobileInboxFilterOptions filterOptions = filterOptions();

        mobileInboxImpl.fetchInbox(givenExternalUserId, filterOptions, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(1)).fetchInbox(null, givenExternalUserId, filterOptions, inboxResultListener);
    }

    @Test
    public void fetchInbox_should_be_called_with_provided_token() {
        MobileInboxFilterOptions filterOptions = filterOptions();

        mobileInboxImpl.fetchInbox(givenToken, givenExternalUserId, filterOptions, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(1)).fetchInbox(givenToken, givenExternalUserId, filterOptions, inboxResultListener);
    }

    @Test
    public void fetchInbox_should_not_be_called_when_externalUserID_is_missing() {
        MobileInboxFilterOptions filterOptions = filterOptions();

        mobileInboxImpl.fetchInbox(null, filterOptions, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(0)).fetchInbox(any(), any(), any(), any());
    }

    @Test
    public void fetchInbox_should_be_called_without_filter_options() {
        mobileInboxImpl.fetchInbox(givenExternalUserId, null, inboxResultListener);

        Mockito.verify(mobileInboxSynchronizer, times(1)).fetchInbox(null, givenExternalUserId, null, inboxResultListener);
    }

    @Test
    public void setSeen_should_not_be_called_when_externalUserID_is_missing() {
        mobileInboxImpl.setSeen(seenResultListener, null, "someMessageId1");

        Mockito.verify(inboxSeenStatusReporter, times(0)).reportSeen(any(), any(), any());
    }

    @Test
    public void setSeen_should_not_be_called_when_messageIDs_are_missing() {
        mobileInboxImpl.setSeen(seenResultListener, givenExternalUserId);

        Mockito.verify(inboxSeenStatusReporter, times(0)).reportSeen(any(), any(), any());
    }

    private MobileInboxFilterOptions filterOptions() {
        return new MobileInboxFilterOptions(
                null, null, givenTopic, 15
        );
    }
}
