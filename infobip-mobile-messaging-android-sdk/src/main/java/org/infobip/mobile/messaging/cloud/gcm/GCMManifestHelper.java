package org.infobip.mobile.messaging.cloud.gcm;

import android.content.Context;

import org.infobip.mobile.messaging.cloud.firebase.MobileMessagingFirebaseService;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
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
        ComponentUtil.setState(context, true, MobileMessagingGcmReceiver.class);
        ComponentUtil.setState(context, true, MobileMessagingInstanceIDListenerService.class);
        ComponentUtil.setState(context, false, MobileMessagingFirebaseService.class);
        try {
            ComponentUtil.setState(context, false, Class.forName("com.google.firebase.iid.FirebaseInstanceIdReceiver"));
            MobileMessagingLogger.w("Disabled com.google.firebase.iid.FirebaseInstanceIdReceiver for compatibility reasons");
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            MobileMessagingLogger.d("Cannot disable FirebaseInstanceIdReceiver: ", e);
        }
    }
}
