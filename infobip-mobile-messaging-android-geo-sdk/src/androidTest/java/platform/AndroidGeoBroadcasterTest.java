package platform;

import android.content.Intent;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEvent;
import org.infobip.mobile.messaging.geo.GeoEventSettings;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoMessage;
import org.infobip.mobile.messaging.geo.platform.AndroidGeoBroadcaster;
import org.infobip.mobile.messaging.geo.report.GeoReport;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;

/**
 * @author sslavin
 * @since 13/03/2017.
 */

public class AndroidGeoBroadcasterTest extends MobileMessagingTestCase {

    private AndroidGeoBroadcaster broadcastSender;
    private ArgumentCaptor<Intent> intentArgumentCaptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        broadcastSender = new AndroidGeoBroadcaster(contextMock);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    }

    @Test
    public void test_should_send_geo_event() {
        // Given
        Geo geo = new Geo(0.0, 0.0, null, null, null, "SomeCampaignId", Collections.singletonList(createArea("areaId1")), new ArrayList<GeoEventSettings>());
        Message message = createMessage(context, "SomeMessageId", false, geo);

        // When
        broadcastSender.geoEvent(GeoEventType.entry, GeoMessage.createFrom(message, geo));

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(GeoEvent.GEOFENCE_AREA_ENTERED.getKey(), intent.getAction());

        GeoMessage geoMessage = GeoMessage.createFrom(intent.getExtras());
        assertNotSame(message, geoMessage);
        assertEquals("SomeMessageId", geoMessage.getMessageId());
    }

    @Test
    public void test_should_send_geo_reports() throws Exception {

        // Given
        GeoReport report = createReport(context, "SomeSignalingMessageId", "SomeCampaignId", "SomeSDKMessageId", false);

        // When
        broadcastSender.geoReported(Collections.singletonList(report));

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(GeoEvent.GEOFENCE_EVENTS_REPORTED.getKey(), intent.getAction());

        List<GeoReport> reportsAfter = GeoReport.createFrom(intent.getExtras());
        assertEquals(1, reportsAfter.size());
        assertJEquals(report, reportsAfter.get(0));
    }

    @Test
    public void test_should_send_error() throws Exception {
        // Given
        MobileMessagingError error = new MobileMessagingError("SomeCode", "SomeMessage");

        // When
        broadcastSender.error(error);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.API_COMMUNICATION_ERROR.getKey(), intent.getAction());

        MobileMessagingError errorAfter = (MobileMessagingError) intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION);
        assertJEquals(error, errorAfter);
    }
}
