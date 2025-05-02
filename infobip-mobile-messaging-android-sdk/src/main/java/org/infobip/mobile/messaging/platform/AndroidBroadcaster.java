package org.infobip.mobile.messaging.platform;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.InstallationMapper;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserMapper;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;

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
    public void notificationDisplayed(Message message, int notificationId) {
        send(prepare(Event.NOTIFICATION_DISPLAYED)
                .putExtras(MessageBundleMapper.messageToBundle(message))
                .putExtra(BroadcastParameter.EXTRA_NOTIFICATION_ID, notificationId));
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
    public void tokenReceived(String cloudToken) {
        send(prepare(Event.TOKEN_RECEIVED)
                .putExtra(BroadcastParameter.EXTRA_CLOUD_TOKEN, cloudToken));
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
    public void userUpdated(User user) {
        send(prepare(Event.USER_UPDATED)
                .putExtras(UserMapper.toBundle(BroadcastParameter.EXTRA_USER, user)));
    }

    @Override
    public void installationUpdated(Installation installation) {
        send(prepare(Event.INSTALLATION_UPDATED)
                .putExtras(InstallationMapper.toBundle(BroadcastParameter.EXTRA_INSTALLATION, installation)));
    }

    @Override
    public void registrationCreated(String cloudToken, String pushRegistrationId) {
        send(prepare(Event.REGISTRATION_CREATED)
                .putExtra(BroadcastParameter.EXTRA_CLOUD_TOKEN, cloudToken)
                .putExtra(BroadcastParameter.EXTRA_INFOBIP_ID, pushRegistrationId));
    }

    @Override
    public void depersonalized() {
        send(prepare(Event.DEPERSONALIZED));
    }

    @Override
    public void personalized(User user) {
        send(prepare(Event.PERSONALIZED)
                .putExtras(UserMapper.toBundle(BroadcastParameter.EXTRA_USER, user)));
    }

    @Override
    public void userSessionsReported() {
        send(prepare(Event.USER_SESSIONS_SENT));
    }

    @Override
    public void customEventsReported() {
        send(prepare(Event.CUSTOM_EVENTS_SENT));
    }

    @Override
    public void inAppClickReported(@NonNull String... clickUrls) {
        if (clickUrls.length == 0) {
            return;
        }

        Intent inAppClickReported = prepare(Event.IN_APP_CLICKS_REPORTED);
        Bundle extras = new Bundle();
        extras.putStringArray(BroadcastParameter.EXTRA_MESSAGE_IDS, clickUrls);
        inAppClickReported.putExtras(extras);
        send(inAppClickReported);
    }

    @Override
    public void userDataJwtExpired() {
        send(prepare(Event.USER_DATA_JWT_EXPIRED));
    }

    private void send(Intent intent) {
        try {
            context.sendBroadcast(intent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception ex) {
            MobileMessagingLogger.e("Failed to send broadcast for action " + intent.getAction() + " due to exception " + ex.getMessage());
        }
    }

    private Intent prepare(Event event) {
        return prepare(event.getKey());
    }

    private Intent prepare(String event) {
        return new Intent(event)
                .setPackage(context.getPackageName());
    }
}
