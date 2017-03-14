package org.infobip.mobile.messaging.geo;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.InfobipAndroidTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author sslavin
 * @since 15/02/2017.
 */

public class GeoHandlerTests extends InfobipAndroidTestCase {

    private GeoAreasHandler handler;
    private ArgumentCaptor<Message> captor;

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        handler = new GeoAreasHandler(context, broadcaster);
        captor = ArgumentCaptor.forClass(Message.class);
    }

    public void test_shouldProvide_MESSAGE_RECEIVED_forGeoTransition() throws Exception {

        // Given
        Geo geo = createGeo(1.0, 2.0, "campaignId", createArea("areaId"));
        Message m = createMessage(context, "SomeMessageId", true, geo);
        GeoTransition transition = GeoHelper.createTransition("areaId");

        // When
        handler.handleTransition(transition);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).messageReceived(captor.capture());
        Message message = captor.getValue();
        assertNotSame("SomeMessageId", message.getMessageId());
        assertEquals("campaignId", message.getGeo().getCampaignId());
        assertEquals("areaId", message.getGeo().getAreasList().get(0).getId());
    }

    public void test_shouldCompareRadiusInGeoAreasList() {
        final Area area1 = createArea("areaId1", "", 1.0, 2.0, 700);
        final Area area2 = createArea("areaId2", "", 1.0, 2.0, 250);
        final Area area3 = createArea("areaId3", "", 1.0, 2.0, 1000);
        List<Area> areasList = new ArrayList<Area>() {{
            add(area1);
            add(area2);
            add(area3);
        }};

        GeoReportHelper.GeoAreaRadiusComparator geoAreaRadiusComparator = new GeoReportHelper.GeoAreaRadiusComparator();
        Collections.sort(areasList, geoAreaRadiusComparator);

        assertEquals(areasList.get(0).getId(), area2.getId());
        assertEquals(areasList.get(0).getRadius(), area2.getRadius());
        assertEquals(areasList.get(1).getId(), area1.getId());
        assertEquals(areasList.get(1).getRadius(), area1.getRadius());
        assertEquals(areasList.get(2).getId(), area3.getId());
        assertEquals(areasList.get(2).getRadius(), area3.getRadius());
    }
}
