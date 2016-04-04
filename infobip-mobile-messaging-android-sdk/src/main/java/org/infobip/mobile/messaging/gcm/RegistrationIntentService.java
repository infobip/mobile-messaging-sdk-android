package org.infobip.mobile.messaging.gcm;

import android.app.IntentService;
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
 * @since 21.03.2016.
 */
public class RegistrationIntentService extends IntentService {
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(MobileMessaging.getInstance(this).getGcmSenderId(), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Intent registrationComplete = new Intent(Event.REGISTRATION_ACQUIRED.getKey());
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
            sendRegistrationToServer(token);
            subscribeTopics(token);
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
    private void sendRegistrationToServer(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        MobileMessaging mobileMessaging = MobileMessaging.getInstance(this);
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
        mobileMessaging.reportRegistration();
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
}
