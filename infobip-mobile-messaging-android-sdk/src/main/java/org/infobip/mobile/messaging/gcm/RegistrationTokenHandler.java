package org.infobip.mobile.messaging.gcm;

import android.content.Context;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.util.StringUtils;

import java.io.IOException;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
class RegistrationTokenHandler {
    private static final String[] TOPICS = {"global"};

    void handleRegistrationTokenUpdate(Context context) {
        String senderId = MobileMessagingCore.getGcmSenderId(context);
        if (StringUtils.isBlank(senderId)) {
            return;
        }

        try {
            InstanceID instanceID = InstanceID.getInstance(context);
            String token = instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            MobileMessagingLogger.v(MobileMessagingLogger.TAG, "RECEIVED TOKEN", token);
            new AndroidBroadcaster(context).registrationAcquired(token);
            sendRegistrationToServer(context, token);
            subscribeTopics(context, token);
        } catch (IOException e) {
            MobileMessagingLogger.e(InternalSdkError.ERROR_TOKEN_REFRESH.get(), e);
        }
    }

    /**
     * Persist registration to third-party servers.
     * <p>
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(Context context, String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        String infobipRegistrationId = mobileMessagingCore.getPushRegistrationId();

        boolean saveNeeded = null == infobipRegistrationId ||
                null == mobileMessagingCore.getCloudToken() ||
                !token.equals(mobileMessagingCore.getCloudToken()) ||
                !mobileMessagingCore.isRegistrationIdReported();

        if (saveNeeded) {
            mobileMessagingCore.setRegistrationId(token);
        }

        mobileMessagingCore.sync();
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    private void subscribeTopics(Context context, String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(context);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }

    /**
     * Cleanup GCM token associated with a specific GCM sender ID.
     *
     * @param gcmSenderID GCM sender ID
     */
    void handleRegistrationTokenCleanup(Context context, String gcmSenderID) {
        try {
            InstanceID instanceID = InstanceID.getInstance(context);
            instanceID.deleteToken(gcmSenderID, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
        } catch (IOException e) {
            MobileMessagingLogger.e(InternalSdkError.ERROR_GCM_TOKEN_CLEANUP.get(), e);
        }
    }
}
