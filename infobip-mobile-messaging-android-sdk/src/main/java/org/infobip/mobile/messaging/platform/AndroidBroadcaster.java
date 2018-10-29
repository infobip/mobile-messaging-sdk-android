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
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;

import java.util.List;

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
        send(prepare(Event.MESSAGE_RECEIVED)
                .putExtras(MessageBundleMapper.messageToBundle(message)));
    }

    @Override
    public void notificationTapped(Message message) {
        send(prepare(Event.NOTIFICATION_TAPPED)
                .putExtras(MessageBundleMapper.messageToBundle(message)));
    }

    @Override
    public void error(@NonNull MobileMessagingError error) {
        send(prepare(Event.API_COMMUNICATION_ERROR)
                .putExtra(BroadcastParameter.EXTRA_EXCEPTION, error));
    }

    @Override
    public void registrationAcquired(String cloudToken) {
        send(prepare(Event.REGISTRATION_ACQUIRED)
                .putExtra(BroadcastParameter.EXTRA_CLOUD_TOKEN, cloudToken));
    }

    @Override
    public void registrationCreated(String cloudToken, String pushRegistrationId) {
        send(prepare(Event.REGISTRATION_CREATED)
                .putExtra(BroadcastParameter.EXTRA_CLOUD_TOKEN, cloudToken)
                .putExtra(BroadcastParameter.EXTRA_INFOBIP_ID, pushRegistrationId));
    }

    @Override
    public void registrationEnabled(String cloudToken, String deviceInstanceId, Boolean registrationEnabled) {
        send(prepare(Event.PUSH_REGISTRATION_ENABLED)
                .putExtra(BroadcastParameter.EXTRA_CLOUD_TOKEN, cloudToken)
                .putExtra(BroadcastParameter.EXTRA_INFOBIP_ID, deviceInstanceId)
                .putExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, registrationEnabled));
    }

    @Override
    public void deliveryReported(@NonNull String... messageIds) {
        if (messageIds.length == 0) {
            return;
        }

        Intent deliveryReportsSent = prepare(Event.DELIVERY_REPORTS_SENT);
        Bundle extras = new Bundle();
        extras.putStringArray(BroadcastParameter.EXTRA_MESSAGE_IDS, messageIds);
        deliveryReportsSent.putExtras(extras);
        send(deliveryReportsSent);
    }

    @Override
    public void seenStatusReported(@NonNull String... messageIds) {
        if (messageIds.length == 0) {
            return;
        }

        Intent seenReportsSent = prepare(Event.SEEN_REPORTS_SENT);
        Bundle extras = new Bundle();
        extras.putStringArray(BroadcastParameter.EXTRA_MESSAGE_IDS, messageIds);
        seenReportsSent.putExtras(extras);
        send(seenReportsSent);
    }

    @Override
    public void messagesSent(List<Message> messages) {
        send(prepare(Event.MESSAGES_SENT)
                .putParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES, MessageBundleMapper.messagesToBundles(messages)));
    }

    @Override
    public void userDataReported(UserData userData) {
        send(prepare(Event.USER_DATA_REPORTED)
                .putExtra(BroadcastParameter.EXTRA_USER_DATA, userData.toString()));
    }

    @Override
    public void systemDataReported(SystemData systemData) {
        send(prepare(Event.SYSTEM_DATA_REPORTED)
                .putExtra(BroadcastParameter.EXTRA_SYSTEM_DATA, systemData.toString()));
    }

    @Override
    public void userLoggedOut() {
        send(prepare(Event.USER_LOGGED_OUT));
    }

    @Override
    public void primarySettingChanged(boolean primary) {
        send(prepare(Event.PRIMARY_CHANGED)
                .putExtra(BroadcastParameter.EXTRA_IS_PRIMARY, primary));
    }

    private void send(Intent intent) {
        context.sendBroadcast(intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private Intent prepare(Event event) {
        return prepare(event.getKey());
    }

    private Intent prepare(String event) {
        return new Intent(event)
                .setPackage(context.getPackageName());
    }
}
