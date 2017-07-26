package org.infobip.mobile.messaging.platform;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.LocalEvent;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.dal.bundle.InteractiveCategoryBundleMapper;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.notification.InteractiveCategory;

import java.util.List;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TRIGGERED_ACTION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TRIGGERED_CATEGORY;

/**
 * @author sslavin
 * @since 13/03/2017.
 */

public class AndroidBroadcaster implements Broadcaster {

    private final Context context;

    public AndroidBroadcaster(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void messageReceived(@NonNull Message message) {
        Intent messageReceived = prepareIntent(Event.MESSAGE_RECEIVED);
        messageReceived.putExtras(MessageBundleMapper.messageToBundle(message));
        LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
        context.sendBroadcast(messageReceived);
    }

    @Override
    public void geoMessageReceived(Message message) {
        Intent messageReceived = prepareIntent(LocalEvent.GEO_MESSAGE_RECEIVED.getKey());
        messageReceived.putExtras(MessageBundleMapper.messageToBundle(message));
        LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
        context.sendBroadcast(messageReceived);
    }

    @Override
    public void notificationTapped(Message message) {
        Intent notificationTapped = prepareIntent(Event.NOTIFICATION_TAPPED);
        notificationTapped.putExtras(MessageBundleMapper.messageToBundle(message));
        LocalBroadcastManager.getInstance(context).sendBroadcast(notificationTapped);
        context.sendBroadcast(notificationTapped);
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
    public void deliveryReported(@NonNull String... messageIds) {
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
    public void seenStatusReported(@NonNull String... messageIds) {
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
        messagesSent.putParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES, MessageBundleMapper.messagesToBundles(messages));
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

    @Override
    public void notificationActionTriggered(InteractiveCategory category, String actionId) {
        Intent actionTriggered = prepareIntent(Event.NOTIFICATION_ACTION_CLICKED);
        actionTriggered.putExtra(EXTRA_TRIGGERED_ACTION_ID, actionId);
        actionTriggered.putExtra(EXTRA_TRIGGERED_CATEGORY, InteractiveCategoryBundleMapper.interactiveCategoryToBundle(category));
        context.sendBroadcast(actionTriggered);
        LocalBroadcastManager.getInstance(context).sendBroadcast(actionTriggered);
    }

    private Intent prepareIntent(Event event) {
        return prepareIntent(event.getKey());
    }

    private Intent prepareIntent(String event) {
        return new Intent(event)
                .setPackage(context.getPackageName());
    }
}
