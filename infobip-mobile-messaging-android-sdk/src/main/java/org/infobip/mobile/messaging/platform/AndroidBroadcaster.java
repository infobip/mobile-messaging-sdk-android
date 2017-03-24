package org.infobip.mobile.messaging.platform;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.dal.bundle.BundleMapper;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 13/03/2017.
 */

public class AndroidBroadcaster implements Broadcaster {

    private static Map<GeoEventType, Event> eventBroadcasts = new HashMap<GeoEventType, Event>() {{
        put(GeoEventType.entry, Event.GEOFENCE_AREA_ENTERED);
    }};

    private final Context context;

    public AndroidBroadcaster(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void messageReceived(@NonNull Message message) {
        Intent messageReceived = prepareIntent(Event.MESSAGE_RECEIVED);
        messageReceived.putExtras(BundleMapper.messageToBundle(message));
        LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
        context.sendBroadcast(messageReceived);
    }

    @Override
    public void geoEvent(@NonNull GeoEventType event, @NonNull Message message, @NonNull Geo geo) {
        Event broadcastEvent = eventBroadcasts.get(event);
        if (broadcastEvent == null) {
            return;
        }

        Intent geofenceIntent = prepareIntent(broadcastEvent);
        geofenceIntent.putExtras(BundleMapper.geoToBundle(geo));
        geofenceIntent.putExtras(BundleMapper.messageToBundle(message));
        LocalBroadcastManager.getInstance(context).sendBroadcast(geofenceIntent);
        context.sendBroadcast(geofenceIntent);
    }

    @Override
    public void geoReported(@NonNull List<GeoReport> reports) {
        if (reports.isEmpty()) {
            return;
        }

        Intent geoReportsSent = prepareIntent(Event.GEOFENCE_EVENTS_REPORTED);
        geoReportsSent.putExtras(BundleMapper.geoReportsToBundle(reports));
        context.sendBroadcast(geoReportsSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(geoReportsSent);
    }

    @Override
    public void error(@NonNull MobileMessagingError error) {
        Intent reportingError = prepareIntent(Event.API_COMMUNICATION_ERROR);
        reportingError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, error);
        context.sendBroadcast(reportingError);
        LocalBroadcastManager.getInstance(context).sendBroadcast(reportingError);
    }

    @Override
    public void registrationAcquired(String cloudToken) {
        Intent registrationComplete = prepareIntent(Event.REGISTRATION_ACQUIRED);
        registrationComplete.putExtra(BroadcastParameter.EXTRA_GCM_TOKEN, cloudToken);
        context.sendBroadcast(registrationComplete);
        LocalBroadcastManager.getInstance(context).sendBroadcast(registrationComplete);
    }

    @Override
    public void registrationCreated(String cloudToken, String deviceApplicationInstanceId) {
        Intent registrationCreated = prepareIntent(Event.REGISTRATION_CREATED);
        registrationCreated.putExtra(BroadcastParameter.EXTRA_GCM_TOKEN, cloudToken);
        registrationCreated.putExtra(BroadcastParameter.EXTRA_INFOBIP_ID, deviceApplicationInstanceId);
        context.sendBroadcast(registrationCreated);
        LocalBroadcastManager.getInstance(context).sendBroadcast(registrationCreated);
    }

    @Override
    public void registrationEnabled(String cloudToken, String deviceInstanceId, Boolean registrationEnabled) {
        Intent registrationUpdated = prepareIntent(Event.PUSH_REGISTRATION_ENABLED);
        registrationUpdated.putExtra(BroadcastParameter.EXTRA_GCM_TOKEN, cloudToken);
        registrationUpdated.putExtra(BroadcastParameter.EXTRA_INFOBIP_ID, deviceInstanceId);
        registrationUpdated.putExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, registrationEnabled);
        context.sendBroadcast(registrationUpdated);
        LocalBroadcastManager.getInstance(context).sendBroadcast(registrationUpdated);
    }

    @Override
    public void deliveryReported(@NonNull String...messageIds) {
        if (messageIds.length == 0) {
            return;
        }

        Intent deliveryReportsSent = prepareIntent(Event.DELIVERY_REPORTS_SENT);
        Bundle extras = new Bundle();
        extras.putStringArray(BroadcastParameter.EXTRA_MESSAGE_IDS, messageIds);
        deliveryReportsSent.putExtras(extras);
        context.sendBroadcast(deliveryReportsSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(deliveryReportsSent);
    }

    @Override
    public void seenStatusReported(@NonNull String...messageIds) {
        if (messageIds.length == 0) {
            return;
        }

        Intent seenReportsSent = prepareIntent(Event.SEEN_REPORTS_SENT);
        Bundle extras = new Bundle();
        extras.putStringArray(BroadcastParameter.EXTRA_MESSAGE_IDS, messageIds);
        seenReportsSent.putExtras(extras);
        context.sendBroadcast(seenReportsSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(seenReportsSent);
    }

    @Override
    public void messagesSent(List<Message> messages) {
        Intent messagesSent = prepareIntent(Event.MESSAGES_SENT);
        messagesSent.putParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES, BundleMapper.messagesToBundles(messages));
        context.sendBroadcast(messagesSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(messagesSent);
    }

    @Override
    public void userDataReported(UserData userData) {
        Intent userDataReported = prepareIntent(Event.USER_DATA_REPORTED);
        userDataReported.putExtra(BroadcastParameter.EXTRA_USER_DATA, userData.toString());
        context.sendBroadcast(userDataReported);
        LocalBroadcastManager.getInstance(context).sendBroadcast(userDataReported);
    }

    @Override
    public void systemDataReported(SystemData systemData) {
        Intent dataReported = prepareIntent(Event.SYSTEM_DATA_REPORTED);
        dataReported.putExtra(BroadcastParameter.EXTRA_SYSTEM_DATA, systemData.toString());
        context.sendBroadcast(dataReported);
        LocalBroadcastManager.getInstance(context).sendBroadcast(dataReported);
    }

    private Intent prepareIntent(Event event) {
        return new Intent(event.getKey())
                .setPackage(context.getPackageName());
    }
}
