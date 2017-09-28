package org.infobip.mobile.messaging.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * @author sslavin
 * @since 25/05/16.
 */
public class MobileMessagingInstanceIDListenerService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        Intent intent = new Intent(this, MobileMessagingGcmIntentService.class);
        intent.setAction(MobileMessagingGcmIntentService.ACTION_ACQUIRE_INSTANCE_ID);
        MobileMessagingGcmIntentService.enqueueWork(this, intent);
    }
}
