package org.infobip.mobile.messaging.cloud.firebase;

import android.content.Context;

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

    private static FirebaseMessageMapper messageMapper;

    public static FirebaseMessageMapper getMessageMapper() {
        if (null == messageMapper) {
            messageMapper = new FirebaseMessageMapper();
        }
        return messageMapper;
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        onMessageReceived(this, remoteMessage);
    }

    @Override
    public void onNewToken(String token) {
        onNewToken(this, token);
    }

    public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage) {
        Message message = getMessageMapper().createMessage(remoteMessage);
        MobileMessagingLogger.v(TAG, "RECEIVED MESSAGE FROM FCM", message);
        if (message != null) {
            MobileMessagingCloudService.enqueueNewMessage(context, message);
            return true;
        } else {
            MobileMessagingLogger.w("Cannot process message");
            return false;
        }
    }

    public static void onNewToken(Context context, String token) {
        MobileMessagingLogger.v(TAG, "RECEIVED TOKEN FROM FCM", token);
        String senderId = MobileMessagingCore.getSenderId(context);
        MobileMessagingCloudService.enqueueNewToken(context, senderId, token);
    }
}
