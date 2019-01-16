package org.infobip.mobile.messaging.cloud.gcm;

import android.content.Context;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.cloud.RegistrationTokenHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.platform.Broadcaster;

import java.io.IOException;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
@Deprecated
public class GCMRegistrationTokenHandler extends RegistrationTokenHandler {
    private static final String[] TOPICS = {"global"};

    private final Broadcaster broadcaster;
    private final Context context;

    public GCMRegistrationTokenHandler(MobileMessagingCore mobileMessagingCore, Broadcaster broadcaster, Context context) {
        super(mobileMessagingCore);
        this.broadcaster = broadcaster;
        this.context = context;
    }

    @Override
    public void handleNewToken(String senderId, String ignored) {
        try {
            InstanceID instanceID = InstanceID.getInstance(context);
            String token = instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            MobileMessagingLogger.v(MobileMessagingLogger.TAG, "RECEIVED TOKEN", token);
            broadcaster.tokenReceived(token);
            sendRegistrationToServer(token);
            subscribeTopics(token);
        } catch (IOException e) {
            MobileMessagingLogger.e(InternalSdkError.ERROR_TOKEN_REFRESH.get(), e);
        }
    }

    @Override
    public void cleanupToken(String senderId) {
        try {
            InstanceID instanceID = InstanceID.getInstance(context);
            instanceID.deleteToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
        } catch (IOException e) {
            MobileMessagingLogger.e(InternalSdkError.ERROR_GCM_TOKEN_CLEANUP.get(), e);
        }
    }

    @Override
    public void acquireNewToken(String senderId) {
        try {
            InstanceID instanceID = InstanceID.getInstance(context);
            String token = instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            MobileMessagingLogger.v(MobileMessagingLogger.TAG, "RECEIVED TOKEN AFTER RESET", token);
            broadcaster.tokenReceived(token);
            sendRegistrationToServer(token);
            subscribeTopics(token);
        } catch (IOException e) {
            MobileMessagingLogger.e(InternalSdkError.ERROR_GCM_TOKEN_CLEANUP.get(), e);
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(context);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
}
