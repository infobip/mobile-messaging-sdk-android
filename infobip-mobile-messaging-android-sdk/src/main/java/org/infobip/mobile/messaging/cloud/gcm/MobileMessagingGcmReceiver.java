package org.infobip.mobile.messaging.cloud.gcm;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.gcm.GcmReceiver;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.cloud.MobileMessagingCloudService;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

/**
 * Receives GCM push notifications and triggers processing in {@link MobileMessagingCloudService}.
 * <p>
 * To be able to use it you must register it as a receiver in AndroidManifest.xml
 * <pre>
 * {@code <receiver android:name="org.infobip.mobile.messaging.gcm.MobileMessagingGcmReceiver"
 *             android:exported="true"
 *             android:permission="com.google.android.c2dm.permission.SEND">
 *       <intent-filter>
 *           <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
 *       </intent-filter>
 *   </receiver>
 *   <service android:name="org.infobip.mobile.messaging.gcm.MobileMessagingGcmIntentService"
 *             android:exported="false">
 *   </service>
 *   }
 * </pre>
 * <p>
 *
 * @author mstipanov
 * @see GcmReceiver
 * @since 21.03.2016.
 */
@Deprecated
public class MobileMessagingGcmReceiver extends GcmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Message message = GCMMessageMapper.fromCloudBundle(intent.getExtras());
        MobileMessagingLogger.v("RECEIVED MESSAGE FROM GCM", message);
        MobileMessagingCloudService.enqueueNewMessage(context, message);

        if (isOrderedBroadcast()) {
            abortBroadcast();
        }
    }
}
