package org.infobip.mobile.messaging.cloud.gcm;

import com.google.android.gms.iid.InstanceIDListenerService;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.cloud.MobileMessagingCloudService;

/**
 * @author sslavin
 * @since 25/05/16.
 */
@Deprecated
public class MobileMessagingInstanceIDListenerService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        String senderId = MobileMessagingCore.getSenderId(this);
        MobileMessagingCloudService.enqueueTokenAcquisition(this, senderId);
    }
}
