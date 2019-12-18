package org.infobip.mobile.messaging.cloud.firebase;

import android.content.Context;

import org.infobip.mobile.messaging.util.ComponentUtil;


public class FirebaseManifestHelper {

    public static void verifyAndConfigureManifest(Context context) {
        // this component needs to be enabled for legacy migration reasons for versions that used GCM
        // it was disabled if GCM component was used instead of Firebase and not re-enabled programmatically if Firebase was used again
        ComponentUtil.enableComponent(context, "com.google.firebase.iid.FirebaseInstanceIdReceiver");
        ComponentUtil.enableComponent(context, MobileMessagingFirebaseService.class);
    }
}
