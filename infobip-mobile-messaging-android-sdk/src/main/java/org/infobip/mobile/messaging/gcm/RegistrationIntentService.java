package org.infobip.mobile.messaging.gcm;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;
import org.infobip.mobile.messaging.api.support.util.StringUtils;
import org.infobip.mobile.messaging.tasks.CreateRegistrationTask;
import org.infobip.mobile.messaging.tasks.UpdateRegistrationTask;

import java.io.IOException;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 21.03.2016.
 */
@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class RegistrationIntentService extends IntentService {
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(MobileMessaging.getInstance().getGcmSenderId(), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
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
        MobileMessaging mobileMessaging = MobileMessaging.getInstance();
        String infobipRegistrationId = mobileMessaging.getInfobipRegistrationId();

//        boolean saveNeeded = null == infobipRegistrationId ||
//                null == mobileMessaging.getRegistrationId() ||
//                !token.equals(mobileMessaging.getRegistrationId()) ||
//                !mobileMessaging.isRegistrationIdSaved();
//
//        if (!saveNeeded) {
//            return;
//        }

        mobileMessaging.setRegistrationId(token);
        mobileMessaging.setRegistrationIdSaved(false);
        AsyncTask task;
        if (null == infobipRegistrationId) {
            task = new CreateRegistrationTask() {
                @Override
                protected void onPostExecute(RegistrationResponse registrationResponse) {
                    if (null == registrationResponse || StringUtils.isBlank(registrationResponse.getDeviceApplicationInstanceId())) {
                        Log.e(TAG, "MobileMessaging API didn't return any value!");

                        Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                        LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(registrationSaveError);
                        return;
                    }
                    MobileMessaging.getInstance().setInfobipRegistrationId(registrationResponse.getDeviceApplicationInstanceId());
                    MobileMessaging.getInstance().setRegistrationIdSaved(true);

                    Intent registrationCreated = new Intent(Event.REGISTRATION_CREATED.getKey());
                    LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(registrationCreated);
                }

                @Override
                protected void onCancelled() {
                    MobileMessaging.getInstance().setRegistrationIdSaved(false);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(registrationSaveError);
                }
            };
        } else {
            task = new UpdateRegistrationTask() {
                @Override
                protected void onPostExecute(RegistrationResponse registrationResponse) {
                    if (null == registrationResponse || StringUtils.isBlank(registrationResponse.getDeviceApplicationInstanceId())) {
                        Log.e(TAG, "MobileMessaging API didn't return any value!");

                        Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                        LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(registrationSaveError);
                        return;
                    }
                    MobileMessaging.getInstance().setInfobipRegistrationId(registrationResponse.getDeviceApplicationInstanceId());
                    MobileMessaging.getInstance().setRegistrationIdSaved(true);

                    Intent registrationChanged = new Intent(Event.REGISTRATION_CHANGED.getKey());
                    LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(registrationChanged);
                }

                @Override
                protected void onCancelled() {
                    MobileMessaging.getInstance().setRegistrationIdSaved(false);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    LocalBroadcastManager.getInstance(RegistrationIntentService.this).sendBroadcast(registrationSaveError);
                }
            };
        }
        task.execute();
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
