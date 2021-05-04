package org.infobip.mobile.messaging.cloud.firebase;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.cloud.RegistrationTokenHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.util.StringUtils;

import java.io.IOException;

/**
 * @author sslavin
 * @since 03/09/2018.
 */
public class FirebaseRegistrationTokenHandler extends RegistrationTokenHandler {

    private static final String TAG = FirebaseRegistrationTokenHandler.class.getSimpleName();

    private final Broadcaster broadcaster;

    public FirebaseRegistrationTokenHandler(MobileMessagingCore mobileMessagingCore, Broadcaster broadcaster) {
        super(mobileMessagingCore);
        this.broadcaster = broadcaster;
    }

    public void handleNewToken(String senderId, String token) {
        if (StringUtils.isBlank(token)) {
            MobileMessagingLogger.w("Not processing empty FCM token");
            return;
        }
        MobileMessagingLogger.v(TAG, "RECEIVED FCM TOKEN", token);
        broadcaster.tokenReceived(token);
        sendRegistrationToServer(token);
    }

    public void cleanupToken(String senderId) {
        if (StringUtils.isBlank(senderId)) {
            return;
        }

        try {
            FirebaseInstanceId.getInstance().deleteToken(senderId, FirebaseMessaging.INSTANCE_ID_SCOPE);
        } catch (IOException e) {
            MobileMessagingLogger.e(TAG, "Error while deleting token", e);
        }
    }

    public void acquireNewToken(final String senderId) {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                final String token = instanceIdResult.getToken();
                handleNewToken(senderId, token);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                MobileMessagingLogger.e(TAG, "Error while acquiring token", e);
            }
        });
    }
}
