package org.infobip.mobile.messaging.gcm;

import android.content.Context;
import android.content.Intent;
import com.google.android.gms.gcm.GcmReceiver;

/**
 * Receives GCM push notifications and triggers processing in {@link MobileMessagingGcmIntentService}.
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
 * @see MobileMessagingGcmIntentService
 * @since 21.03.2016.
 */
public class MobileMessagingGcmReceiver extends GcmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Intent service = new Intent(MobileMessagingGcmIntentService.ACTION_GCM_MESSAGE_RECEIVE, null, context, MobileMessagingGcmIntentService.class);
        service.putExtras(intent);
        context.startService(service);

        if (isOrderedBroadcast()) {
            abortBroadcast();
        }
    }
}
