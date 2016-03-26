package org.infobip.mobile.messaging.gcm;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

public class MobileMessagingInstanceIDListenerService extends InstanceIDListenerService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}