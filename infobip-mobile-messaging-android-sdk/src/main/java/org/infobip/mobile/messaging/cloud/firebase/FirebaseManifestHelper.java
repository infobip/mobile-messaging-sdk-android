/*
 * FirebaseManifestHelper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.cloud.firebase;

import android.content.Context;

import org.infobip.mobile.messaging.util.ComponentUtil;

public class FirebaseManifestHelper {

    /**
     * @deprecated it's left because we still have installations of library version less than 2.1.0
     * Could be deleted if we don't have such installations and almost all users already updated.
     */
    @Deprecated
    public static void verifyAndConfigureManifest(Context context) {
        // this component needs to be enabled for legacy migration reasons for versions that used GCM
        // it was disabled if GCM component was used instead of Firebase and not re-enabled programmatically if Firebase was used again
        ComponentUtil.enableComponent(context, "com.google.firebase.iid.FirebaseInstanceIdReceiver");
        ComponentUtil.enableComponent(context, MobileMessagingFirebaseService.class);
    }
}
