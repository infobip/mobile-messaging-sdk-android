package org.infobip.mobile.messaging.geo.transition;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.MobileGeoImpl;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.platform.GeoBroadcaster;
import org.infobip.mobile.messaging.geo.report.GeoReport;
import org.infobip.mobile.messaging.geo.report.GeoReportHelper;
import org.infobip.mobile.messaging.geo.report.GeoReporter;
import org.infobip.mobile.messaging.geo.report.GeoReportingResult;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author pandric
 * @since 24.06.2016.
 */
public class GeoAreasHandler {

    private static final String TAG = "GeofenceTransitions";

    private final MessageStore geoMessageStore;
    private final GeoNotificationHelper geoNotificationHelper;
    private final GeoReporter geoReporter;
    private final Context context;
    private final GeofencingHelper geofencingHelper;
    private final MobileMessagingCore mobileMessagingCore;

    GeoAreasHandler(Context context, GeoBroadcaster geoBroadcaster) {
        this.context = context;
        this.mobileMessagingCore = MobileMessagingCore.getInstance(context);
        this.geoNotificationHelper = new GeoNotificationHelper(context, geoBroadcaster, new AndroidBroadcaster(context), mobileMessagingCore.getNotificationHandler());
        this.geoReporter = new GeoReporter(context, MobileMessagingCore.getInstance(context), geoBroadcaster,
                MobileMessagingCore.getInstance(context).getStats(), new MobileApiResourceProvider().getMobileApiGeo(context));
        this.geofencingHelper = new GeofencingHelper(context);
        this.geoMessageStore = geofencingHelper.getMessageStoreForGeo();
    }

    public GeoAreasHandler(Context context, MobileMessagingCore mobileMessagingCore, GeoNotificationHelper geoNotificationHelper, GeoReporter geoReporter, GeofencingHelper geofencingHelper) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.geoNotificationHelper = geoNotificationHelper;
        this.geoReporter = geoReporter;
        this.geofencingHelper = geofencingHelper;
        this.geoMessageStore = geofencingHelper.getMessageStoreForGeo();
    }

    /**
     * Handles geofencing transition intent and reports corresponding areas to server
     *
     * @param intent intent from Google Location Services
     */
    void handleTransition(Intent intent) {

        GeoTransition transition;
        try {
            transition = GeoTransitionHelper.resolveTransitionFromIntent(intent);
        } catch (GeoTransitionHelper.GeofenceNotAvailableException e) {
            GeofencingHelper.setAllActiveGeoAreasMonitored(context, false);
            MobileMessagingLogger.e(TAG, "Geofence not available");
            return;
        } catch (Exception e) {
            MobileMessagingLogger.e(TAG, "Cannot resolve transition information: " + e);
            return;
        }

        handleTransition(transition);
    }

    /**
     * Handles geofencing intent and reports corresponding areas to server
     *
     * @param transition resolved transition information
     */
    @SuppressWarnings("WeakerAccess")
    public void handleTransition(GeoTransition transition) {
        MobileMessagingLogger.v("GEO TRANSITION", transition);
        Map<Message, List<Area>> messagesAndAreas = GeoReportHelper.findSignalingMessagesAndAreas(context, geoMessageStore, transition.getRequestIds(), transition.getEventType());
        if (messagesAndAreas.isEmpty()) {
            MobileMessagingLogger.d(TAG, "No messages for triggered areas");
            return;
        }

        logGeofences(messagesAndAreas.values(), transition.getEventType());

        geofencingHelper.addUnreportedGeoEvents(GeoReportHelper.createReportsForMultipleMessages(context, messagesAndAreas, transition.getEventType(), transition.getTriggeringLocation()));
        GeoReport[] unreportedEvents = geofencingHelper.removeUnreportedGeoEvents();
        if (unreportedEvents.length == 0) {
            MobileMessagingLogger.d(TAG, "No geofencing events to report at current time");
            return;
        }

        try {
            GeoReportingResult result = geoReporter.reportSync(unreportedEvents);
            handleReportingResultWithNewMessagesAndNotifications(unreportedEvents, result);
        } catch (Exception e) {
            MobileMessagingLogger.e(TAG, "Failed to report geo events " + e.toString());
            handleGeoReportingResult(context, new GeoReportingResult(e));
        }
    }

    /**
     * Generates new geo messages based on events and result data and also provides broadcasts and notifications.
     *
     * @param unreportedEvents events that occurred and has been reported to the server.
     * @param result           result of reporting that contains non-active campaign data and new message ids.
     */
    private void handleReportingResultWithNewMessagesAndNotifications(GeoReport[] unreportedEvents, GeoReportingResult result) {
        List<GeoReport> reports = GeoReportHelper.filterOutNonActiveReports(context, Arrays.asList(unreportedEvents), result);
        Map<Message, GeoEventType> messages = GeoReportHelper.createMessagesToNotify(context, reports, result);
        saveMessages(messages.keySet());
        handleGeoReportingResult(context, result);
        geoNotificationHelper.notifyAboutGeoTransitions(messages);
        notifyOtherModulesAboutGeoMessages(messages.keySet());
    }

    /**
     * Saves new geo notification messages into message store.
     *
     * @param generatedMessages generated messages to save to message store.
     */
    private void saveMessages(Collection<Message> generatedMessages) {
        if (!mobileMessagingCore.isMessageStoreEnabled()) {
            return;
        }

        MessageStore messageStore = mobileMessagingCore.getMessageStore();
        messageStore.save(context, generatedMessages.toArray(new Message[0]));
    }

    /**
     * Processes geo reporting result and updates any stored data based on it
     *
     * @param result result from the server
     */
    public static void handleGeoReportingResult(Context context, @NonNull GeoReportingResult result) {
        updateMessageStoreWithReportingResult(context, result);
        updateUnreportedSeenMessageIds(context, result);

        if (!result.hasError()) {
            MobileMessagingCore.getInstance(context).sync();
        }
    }

    /**
     * Updates ids of existing messages based on reporting result.
     * <br> Does nothing if message store is not enabled.
     *
     * @param reportingResult geo reporting result that contains mapping for new message ids
     */
    private static void updateMessageStoreWithReportingResult(Context context, @NonNull GeoReportingResult reportingResult) {
        if (reportingResult.getMessageIds() == null || reportingResult.getMessageIds().isEmpty()) {
            return;
        }

        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        if (!mobileMessagingCore.isMessageStoreEnabled()) {
            return;
        }

        MessageStore messageStore = mobileMessagingCore.getMessageStore();
        // Code below is far from being effective but messageId is primary key
        // so we will have to remove messages with invalid keys
        List<Message> allMessages = messageStore.findAll(context);
        Map<String, String> messageIds = reportingResult.getMessageIds();
        for (Message message : allMessages) {
            String newMessageId = messageIds.get(message.getMessageId());
            if (newMessageId == null) {
                continue;
            }

            message.setMessageId(newMessageId);
        }
        messageStore.deleteAll(context);
        messageStore.save(context, allMessages.toArray(new Message[0]));
    }

    /**
     * Updates unreported ids that were marked as seen with the ones provided with report response
     *
     * @param reportingResult result of geo reporting
     */
    private static void updateUnreportedSeenMessageIds(Context context, @NonNull GeoReportingResult reportingResult) {
        if (reportingResult.hasError() ||
                reportingResult.getMessageIds() == null ||
                reportingResult.getMessageIds().isEmpty()) {
            return;
        }

        MobileMessagingCore.getInstance(context).updateUnreportedSeenMessageIds(reportingResult.getMessageIds());
        MobileMessagingCore.getInstance(context).updatedGeneratedMessageIDs(reportingResult.getMessageIds());
    }

    /**
     * Prints geofence details as information log
     *
     * @param collection lists of geofences
     * @param event      transition event type
     */
    private static void logGeofences(Collection<List<Area>> collection, @NonNull GeoEventType event) {
        for (List<Area> areas : collection) {
            for (Area a : areas) {
                MobileMessagingLogger.v(TAG, event.name().toUpperCase() + " (" + a.getTitle() + ") LAT:" + a.getLatitude() + " LON:" + a.getLongitude() + " RAD:" + a.getRadius());
            }
        }
    }

    private void notifyOtherModulesAboutGeoMessages(Collection<Message> messages) {
        for (MessageHandlerModule module : mobileMessagingCore.getMessageHandlerModules()) {
            if (module instanceof MobileGeoImpl) {
                continue;
            }

            for (Message message : messages) {
                module.handleMessage(message);
            }
        }
    }

    @VisibleForTesting
    public MobileMessagingCore getMobileMessagingCore() {
        return mobileMessagingCore;
    }
}