package org.infobip.mobile.messaging.cloud.gcm;

import android.content.Context;

import org.infobip.mobile.messaging.cloud.firebase.MobileMessagingFirebaseService;
import org.infobip.mobile.messaging.util.ComponentUtil;

/**
 * @author sslavin
 * @since 04/09/2018.
 */
@Deprecated
public class GCMManifestHelper {

    public static void verifyAndConfigureManifest(Context context) {
        ComponentUtil.verifyManifestReceiver(context, MobileMessagingGcmReceiver.class);
        ComponentUtil.verifyManifestService(context, MobileMessagingInstanceIDListenerService.class);
        ComponentUtil.enableComponent(context, MobileMessagingGcmReceiver.class);
        ComponentUtil.enableComponent(context, MobileMessagingInstanceIDListenerService.class);
        ComponentUtil.disableComponent(context, MobileMessagingFirebaseService.class);
        ComponentUtil.disableComponent(context, "com.google.firebase.iid.FirebaseInstanceIdReceiver");
    }
}
