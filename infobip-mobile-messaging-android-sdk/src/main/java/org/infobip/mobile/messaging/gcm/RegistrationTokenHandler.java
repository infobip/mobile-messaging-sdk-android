package org.infobip.mobile.messaging.gcm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.util.StringUtils;

import java.io.IOException;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
class RegistrationTokenHandler {
    private static final String[] TOPICS = {"global"};

    void handleRegustrationTokenUpdate(Context context) {
        try {
            InstanceID instanceID = InstanceID.getInstance(context);
            String token = instanceID.getToken(MobileMessaging.getInstance(context).getGcmSenderId(), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Intent registrationComplete = new Intent(Event.REGISTRATION_ACQUIRED.getKey());
            registrationComplete.putExtra("registrationId", token);
            context.sendBroadcast(registrationComplete);
            LocalBroadcastManager.getInstance(context).sendBroadcast(registrationComplete);
            sendRegistrationToServer(context, token);
            subscribeTopics(context, token);
        } catch (IOException e) {
            Log.e(TAG, "Failed to complete token refresh", e);
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

        MobileMessaging mobileMessaging = MobileMessaging.getInstance(context);
        String infobipRegistrationId = mobileMessaging.getDeviceApplicationInstanceId();

        boolean saveNeeded = null == infobipRegistrationId ||
                null == mobileMessaging.getRegistrationId() ||
                !token.equals(mobileMessaging.getRegistrationId()) ||
                !mobileMessaging.isRegistrationIdSaved();

        if (!saveNeeded) {
            return;
        }

        mobileMessaging.setRegistrationId(token);
        mobileMessaging.setRegistrationIdSaved(false);
        mobileMessaging.reportUnreportedRegistration();
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(Context context, String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(context);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
}
