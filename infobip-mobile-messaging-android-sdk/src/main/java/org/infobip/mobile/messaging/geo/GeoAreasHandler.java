package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.geo.GeoReportingResult;
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

    private final MessageStore messageStore;
    private final Context context;

    /**
     * Supported geofence transition events
     */
    private static SparseArray<GeoEventType> supportedTransitionEvents = new SparseArray<GeoEventType>() {{
        put(Geofence.GEOFENCE_TRANSITION_ENTER, GeoEventType.entry);
    }};

    GeoAreasHandler(Context context) {
        this.context = context;
        this.messageStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
    }

    /**
     * Handles geofencing transition and reports corresponding areas to server
     * @param intent intent from Google Locaiton Services
     */
    void handleTransition(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            MobileMessagingLogger.e(TAG, "Geofencing event is null, cannot process");
            return;
        }

        if (geofencingEvent.hasError()) {
            MobileMessagingLogger.e(TAG, "ERROR: " + GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode()));
            return;
        }

        GeoEventType event = supportedTransitionEvents.get(geofencingEvent.getGeofenceTransition());
        if (event == null) {
            MobileMessagingLogger.e(TAG, "Cannot identify event for transition: " + geofencingEvent.getGeofenceTransition());
            return;
        }

        Map<Message, List<Area>> messagesAndAreas = GeoReportHelper.findSignalingMessagesAndAreas(context, messageStore, geofencingEvent.getTriggeringGeofences(), event);
        if (messagesAndAreas.isEmpty()) {
            MobileMessagingLogger.d(TAG, "No messages for triggered areas");
            return;
        }

        logGeofences(messagesAndAreas.values(), event);

        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        Location triggeringLocation = geofencingEvent.getTriggeringLocation();

        mobileMessagingCore.addUnreportedGeoEvents(GeoReportHelper.createReportsForMultipleMessages(messagesAndAreas, event, triggeringLocation));
        GeoReport unreportedEvents[] = MobileMessagingCore.getInstance(context).removeUnreportedGeoEvents();
        if (unreportedEvents.length == 0) {
            MobileMessagingLogger.d(TAG, "No geofencing events to report at current time");
            return;
        }

        GeoReportingResult result = GeoReporter.reportSync(context, unreportedEvents);
        processGeoReportingResult(context, unreportedEvents, result);
    }

    /**
     * Processes geo reporting result, generates necessary notification messages and sends broadcasts
     * @param geoReports reports that were sent to the server
     * @param result result from the server
     */
    public static void processGeoReportingResult(Context context, @NonNull GeoReport geoReports[], @NonNull GeoReportingResult result) {
        List<GeoReport> reports = GeoReportHelper.filterOutNonActiveReports(context, Arrays.asList(geoReports), result);
        Map<GeoReport, Message> messages = GeoReportHelper.createMessagesToNotify(context, reports, result);
        saveAndUpdateMessageStore(context, messages.values(), result);

        GeoNotificationHelper.notifyAboutGeoTransitions(context, messages);
    }

    /**
     * Saves new geo notification messages into message store and also updates ids of existing messages based on reporting result.
     * </p> Does nothing if message store is not enabled.
     * @param messages new messages to save in message store
     * @param reportingResult geo reporting result that contains mapping for new message ids
     */
    private static void saveAndUpdateMessageStore(Context context, Collection<Message> messages, @NonNull GeoReportingResult reportingResult) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        if (!mobileMessagingCore.isMessageStoreEnabled()) {
            return;
        }

        MessageStore messageStore = mobileMessagingCore.getMessageStore();
        messageStore.save(context, messages.toArray(new Message[messages.size()]));
        if (reportingResult.getMessageIds() == null || reportingResult.getMessageIds().isEmpty()) {
            return;
        }

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
        messageStore.save(context, allMessages.toArray(new Message[allMessages.size()]));
    }

    /**
     * Prints geofence details as information log
     * @param collection lists of geofences
     * @param event transition event type
     */
    private static void logGeofences(Collection<List<Area>> collection, @NonNull GeoEventType event) {
        for (List<Area> areas : collection) {
            for (Area a : areas) {
                MobileMessagingLogger.i(TAG, event.name().toUpperCase() + " (" + a.getTitle() + ") LAT:" + a.getLatitude() + " LON:" + a.getLongitude() + " RAD:" + a.getRadius());
            }
        }
    }
}