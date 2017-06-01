package org.infobip.mobile.messaging.mobile.messages;

import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.shaded.google.gson.Gson;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class MessagesSynchronizerTest extends MobileMessagingTestCase {

    private static final int MESSAGE_ID_PARAMETER_LIMIT = 100;

    @Test
    public void should_find_all_messageIDs() {
        String[] mockIDs = new String[120];
        for (int i = 0; i < mockIDs.length; i++) {
            mockIDs[i] = UUID.randomUUID().toString();
        }

        mobileMessagingCore.addSyncMessagesIds(mockIDs);
        String[] messageIDs = mobileMessagingCore.getSyncMessagesIds();

        assertNotNull(messageIDs);
        assertTrue(MESSAGE_ID_PARAMETER_LIMIT >= messageIDs.length);
    }

    @Test
    public void should_find_all_no_duplicates_and_nulls() {
        String mockId = UUID.randomUUID().toString();
        String[] mockIDs = new String[10];
        for (int i = 0; i < mockIDs.length; i++) {
            mockIDs[i] = i % 2 == 0 ? mockId : null;
        }

        mobileMessagingCore.addSyncMessagesIds(mockIDs);
        String[] messageIDs = mobileMessagingCore.getSyncMessagesIds();

        assertNotNull(messageIDs);
        assertTrue(messageIDs.length == 1);
    }

    @Test
    public void should_not_report_dlr_with_duplicate_messageIds() {
        // Given
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        // When
        mobileMessaging.setMessagesDelivered("1");
        mobileMessaging.setMessagesDelivered("2");
        mobileMessaging.setMessagesDelivered("3");
        mobileMessaging.setMessagesDelivered("4");
        mobileMessaging.setMessagesDelivered("5");

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).deliveryReported(Mockito.<String>anyVararg());
        List<String> actualDLRs = getReportedDLRs();
        assertEquals(5, actualDLRs.size());
        assertTrue(actualDLRs.containsAll(Arrays.asList("1", "2", "3", "4", "5")));
    }

    private List<String> getReportedDLRs() {
        Gson gson = new Gson();
        List<String> bodies = debugServer.getBodiesForUri("/mobile/5/messages");
        List<String> ids = new ArrayList<>();
        for (String body : bodies) {
            SyncMessagesBody requestBody = gson.fromJson(body, SyncMessagesBody.class);
            if (requestBody.getDrIDs() != null) {
                ids.addAll(Arrays.asList(requestBody.getDrIDs()));
            }
        }
        return ids;
    }
}
