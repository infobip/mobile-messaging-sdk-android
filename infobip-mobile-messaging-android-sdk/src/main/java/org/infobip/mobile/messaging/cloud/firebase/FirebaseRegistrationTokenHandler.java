package org.infobip.mobile.messaging.cloud.firebase;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.cloud.RegistrationTokenHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.util.StringUtils;

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

    public void handleNewToken(String token) {
        if (StringUtils.isBlank(token)) {
            MobileMessagingLogger.w("Not processing empty FCM token");
            return;
        }
        MobileMessagingLogger.v(TAG, "RECEIVED FCM TOKEN", token);
        broadcaster.tokenReceived(token);
        sendRegistrationToServer(token);
    }

    public void cleanupToken() {
        try {
            FirebaseMessaging.getInstance().deleteToken();
        } catch (Exception e) {
            MobileMessagingLogger.e(TAG, "Error while deleting token", e);
        }
    }

    public void acquireNewToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                handleNewToken(s);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                MobileMessagingLogger.e(TAG, "Error while acquiring token", e);
            }
        });
    }
}
