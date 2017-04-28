package org.infobip.mobile.messaging.geo.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.android.MobileMessagingBaseTestCase;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.SqliteDatabaseProvider;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEventSettings;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoLatLng;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.mapper.GeoDataMapper;
import org.infobip.mobile.messaging.geo.platform.GeoBroadcaster;
import org.infobip.mobile.messaging.geo.report.GeoReport;
import org.infobip.mobile.messaging.geo.storage.TestMessageStore;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.platform.TimeProvider;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 10/03/2017.
 */

public abstract class MobileMessagingTestCase extends MobileMessagingBaseTestCase {

    protected static MessageStore geoStore;
    private static GeofencingHelper geofencingHelper;
    protected DatabaseHelper databaseHelper;
    protected SqliteDatabaseProvider databaseProvider;
    protected GeoBroadcaster geoBroadcaster;
    protected Broadcaster messageBroadcaster;
    protected TestTimeProvider time;
    protected MobileMessagingCore mobileMessagingCore;
    protected MobileMessaging mobileMessaging;

    protected static class TestTimeProvider implements TimeProvider {

        long delta = 0;
        boolean overwritten = false;

        public void forward(long time, TimeUnit unit) {
            delta += unit.toMillis(time);
        }

        public void backward(long time, TimeUnit unit) {
            delta -= unit.toMillis(time);
        }

        public void reset() {
            overwritten = false;
            delta = 0;
        }

        public void set(long time) {
            overwritten = true;
            delta = time;
        }

        @Override
        public long now() {
            if (overwritten) {
                return delta;
            } else {
                return System.currentTimeMillis() + delta;
            }
        }
    }

    protected void enableMessageStoreForReceivedMessages() {
        PreferenceHelper.saveClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, TestMessageStore.class);
    }

    @SuppressLint("ApplySharedPref")
    @Before
    public void setUp() throws Exception {
        super.setUp();

        MobileMessagingLogger.enforce();

        time = new TestTimeProvider();
        Time.reset(time);

        messageBroadcaster = Mockito.mock(Broadcaster.class);
        mobileMessagingCore = MobileMessagingTestable.create(context, messageBroadcaster);
        mobileMessaging = mobileMessagingCore;
        geofencingHelper = new GeofencingHelper(context);

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, "TestRegistrationId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, true);

        MobileApiResourceProvider.INSTANCE.resetMobileApi();

        geoBroadcaster = Mockito.mock(GeoBroadcaster.class);
        geofencingHelper.removeUnreportedGeoEvents();

        databaseHelper = MobileMessagingCore.getDatabaseHelper(context);
        databaseProvider = MobileMessagingCore.getDatabaseProvider(context);
        geoStore = geofencingHelper.getMessageStoreForGeo();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        time.reset();
        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception ignored) {
            }
        }
        databaseProvider.deleteDatabase();
    }

    /**
     * Generates messages with provided id
     *
     * @param saveToStorage set to true to save messages to message store
     * @param messageId     message id for a message
     * @param campaignId    id of a campaign for a message
     * @param areas         list of areas to save to message
     * @return new message
     */
    protected static Message createMessage(Context context, String messageId, String campaignId, boolean saveToStorage, Area... areas) {
        return createMessage(context, messageId, saveToStorage, new Geo(0.0, 0.0, null, null, null, campaignId, Arrays.asList(areas), new ArrayList<GeoEventSettings>()));
    }

    /**
     * Generates messages with provided ids and geo campaign object
     *
     * @param saveToStorage set to true to save messages to message store
     * @param messageId     message id for a message
     * @param geo           geo campaign object
     * @return new message
     */
    protected static Message createMessage(Context context, String messageId, boolean saveToStorage, Geo... geo) {
        Message message = new Message();
        message.setMessageId(messageId);

        boolean isGeo = geo.length > 0 && geo[0] != null && geo[0].getAreasList() != null && !geo[0].getAreasList().isEmpty();
        if (isGeo) {
            message.setInternalData(GeoDataMapper.geoToInternalData(geo[0]));
        }

        if (saveToStorage) {
            geoStore.save(context, message);
        }
        return message;
    }

    /**
     * Generates geo report for the provided data
     *
     * @param signalingMessageId signaling message id for report
     * @param campaignId         campaign id for a report
     * @param sdkMessageId       id of message generated by sdk
     * @param saveAsUnreported   set to true to save report as unreported
     * @param area               area to insert into report
     * @return geo report
     */
    protected static GeoReport createReport(Context context, String signalingMessageId, String campaignId, String sdkMessageId, boolean saveAsUnreported, Area area) {
        GeoReport report = new GeoReport(
                campaignId,
                sdkMessageId,
                signalingMessageId,
                GeoEventType.entry,
                area == null ? new Area("areaId", "areaTitle", 1.0, 2.0, 3) : area,
                1L,
                new GeoLatLng(1.0, 2.0));

        if (saveAsUnreported) {
            geofencingHelper.addUnreportedGeoEvents(report);
        }

        return report;
    }

    /**
     * Generates geo report for the provided data
     *
     * @param signalingMessageId signaling message id for report
     * @param campaignId         campaign id for a report
     * @param sdkMessageId       id of message generated by sdk
     * @param saveAsUnreported   set to true to save report as unreported
     * @return geo report
     */
    protected static GeoReport createReport(Context context, String signalingMessageId, String campaignId, String sdkMessageId, boolean saveAsUnreported) {
        return createReport(context, signalingMessageId, campaignId, sdkMessageId, saveAsUnreported, null);
    }

    /**
     * Generates area
     *
     * @param areaId area id
     * @return new area
     */
    protected static Area createArea(String areaId) {
        return createArea(areaId, "", 1.0, 1.0, 1);
    }

    /**
     * Generates new area
     *
     * @param areaId area id
     * @param title  area name
     * @param lat    center latitude
     * @param lon    center longitude
     * @param radius area radius
     * @return new area
     */
    protected static Area createArea(String areaId, String title, Double lat, Double lon, Integer radius) {
        return new Area(areaId, title, lat, lon, radius);
    }

    /**
     * Generates new Geo object
     *
     * @param triggeringLatitude  latitude that event was triggered with
     * @param triggeringLongitude longitude that event was triggered with
     * @param campaignId          campaign id
     * @param areas               array of areas to include
     * @return new Geo object
     */
    protected static Geo createGeo(Double triggeringLatitude, Double triggeringLongitude, String campaignId, Area... areas) {
        return new Geo(triggeringLatitude, triggeringLongitude, null, null, null, campaignId, Arrays.asList(areas), new ArrayList<GeoEventSettings>());
    }

    /**
     * Generates new Geo object
     *
     * @param triggeringLatitude  latitude that event was triggered with
     * @param triggeringLongitude longitude that event was triggered with
     * @param expiryTime          geo campaign expiration time
     * @param startTime           geo campaign start time
     * @param campaignId          campaign id
     * @param areas               array of areas to include
     * @return new Geo object
     */
    protected static Geo createGeo(Double triggeringLatitude, Double triggeringLongitude, String expiryTime, String startTime, String campaignId, Area... areas) {
        return new Geo(triggeringLatitude, triggeringLongitude, null, expiryTime, startTime, campaignId, Arrays.asList(areas), new ArrayList<GeoEventSettings>());
    }
}
