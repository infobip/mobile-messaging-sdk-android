package org.infobip.mobile.messaging.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.MobileMessagingTestable;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.SqliteDatabaseProvider;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEventSettings;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoLatLng;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.platform.TimeProvider;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * @author sslavin
 * @since 10/03/2017.
 */

@RunWith(AndroidJUnit4.class)
public abstract class MobileMessagingTestCase {

    protected Context context;
    protected Context contextMock;
    protected DebugServer debugServer;
    protected MobileMessaging mobileMessaging;
    protected MobileMessagingTestable mobileMessagingCore;
    protected MessageStore geoStore;
    protected DatabaseHelper databaseHelper;
    protected SqliteDatabaseProvider databaseProvider;
    protected Broadcaster broadcaster;
    protected TestTimeProvider time;

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

    @SuppressWarnings("WeakerAccess")
    public static class TestMessageStore implements MessageStore {

        Map<String, Message> messages = new HashMap<>();

        @Override
        public List<Message> findAll(Context context) {
            return new ArrayList<>(messages.values());
        }

        @Override
        public long countAll(Context context) {
            return messages.values().size();
        }

        @Override
        public void save(Context context, Message... messages) {
            for (Message message : messages) {
                this.messages.put(message.getMessageId(), message);
            }
        }

        @Override
        public void deleteAll(Context context) {
            messages.clear();
        }
    }

    @SuppressLint("ApplySharedPref")
    @Before
    public void setUp() throws Exception {

        MobileMessagingLogger.enforce();

        context = getInstrumentation().getContext();
        contextMock = mockContext(context);

        debugServer = new DebugServer();
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, "{\n" +
                "  \"code\": \"500\",\n" +
                "  \"message\": \"Internal server error\"\n" +
                "}");
        debugServer.start();

        time = new TestTimeProvider();
        Time.reset(time);

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, "TestRegistrationId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, true);

        MobileApiResourceProvider.INSTANCE.resetMobileApi();

        broadcaster = Mockito.mock(Broadcaster.class);
        mobileMessagingCore = MobileMessagingTestable.create(context, broadcaster);
        mobileMessagingCore.removeUnreportedGeoEvents();
        mobileMessaging = mobileMessagingCore;

        databaseHelper = MobileMessagingCore.getDatabaseHelper(context);
        databaseProvider = MobileMessagingCore.getDatabaseProvider(context);
        geoStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
    }

    private static Context mockContext(final Context realContext) {
        Context contextSpy = Mockito.mock(Context.class);

        Mockito.when(contextSpy.getSharedPreferences(Mockito.anyString(), Mockito.anyInt())).thenAnswer(new Answer<SharedPreferences>() {
            @Override
            public SharedPreferences answer(InvocationOnMock invocation) throws Throwable {
                Object arguments[] = invocation.getArguments();
                return realContext.getSharedPreferences((String) arguments[0], (Integer) arguments[1]);
            }
        });
        Mockito.when(contextSpy.getPackageManager()).thenReturn(realContext.getPackageManager());
        Mockito.when(contextSpy.getApplicationInfo()).thenReturn(realContext.getApplicationInfo());
        Mockito.when(contextSpy.getApplicationContext()).thenReturn(realContext.getApplicationContext());
        Mockito.when(contextSpy.getMainLooper()).thenReturn(realContext.getMainLooper());
        Mockito.when(contextSpy.getPackageName()).thenReturn(realContext.getPackageName());
        Mockito.when(contextSpy.getResources()).thenReturn(realContext.getResources());

        return contextSpy;
    }

    protected void enableMessageStoreForReceivedMessages() {
        PreferenceHelper.saveClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, TestMessageStore.class);
    }

    @After
    public void tearDown() throws Exception {
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
            message.setGeo(geo[0]);
        }
        if (saveToStorage) {
            if (isGeo) {
                MobileMessagingCore.getInstance(context).getMessageStoreForGeo().save(context, message);
            } else {
                MobileMessagingCore.getInstance(context).getMessageStore().save(context, message);
            }
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
            MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(report);
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

    /**
     * Asserts that two objects are strictly the same using JSONAssert and JSON serialization
     *
     * @param expected expected object
     * @param actual   actual object
     * @throws Exception if serialization or assertion fails
     */
    protected static <T> void assertJEquals(T expected, T actual) throws Exception {
        JsonSerializer serializer = new JsonSerializer();
        JSONAssert.assertEquals(serializer.serialize(expected), serializer.serialize(actual), true);
    }

    /**
     * Asserts that two objects are not strictly the same using JSONAssert and JSON serialization
     *
     * @param expected expected object
     * @param actual   actual object
     * @throws Exception if serialization or assertion fails
     */
    protected static <T> void assertJNotEquals(T expected, T actual) throws Exception {
        JsonSerializer serializer = new JsonSerializer();
        JSONAssert.assertNotEquals(serializer.serialize(expected), serializer.serialize(actual), true);
    }
}
