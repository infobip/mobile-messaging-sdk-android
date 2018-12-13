package org.infobip.mobile.messaging.cloud.firebase;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.cloud.MobileMessagingCloudService;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

/**
 * @author sslavin
 * @since 03/09/2018.
 */
public class MobileMessagingFirebaseService extends FirebaseMessagingService {

    private final static String TAG = MobileMessagingFirebaseService.class.getSimpleName();

    private final FirebaseMessageMapper messageMapper;

    public MobileMessagingFirebaseService() {
        this(new FirebaseMessageMapper());
    }

    public MobileMessagingFirebaseService(FirebaseMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Message message = messageMapper.createMessage(remoteMessage);
        MobileMessagingLogger.v(TAG, "RECEIVED MESSAGE FROM FCM", message);
        if (message != null) {
            MobileMessagingCloudService.enqueueNewMessage(this, message);
        }
    }

    @Override
    public void onNewToken(String token) {
        MobileMessagingLogger.v(TAG, "RECEIVED TOKEN FROM FCM", token);
        String senderId = MobileMessagingCore.getSenderId(this);
        MobileMessagingCloudService.enqueueNewToken(this, senderId, token);
    }
}
