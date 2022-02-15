package org.infobip.mobile.messaging.geo.tools;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.cloud.firebase.FirebaseAppProvider;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Platform;
import org.infobip.mobile.messaging.util.ModuleLoader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author sslavin
 * @since 13/03/2017.
 */

public class MobileMessagingTestable extends MobileMessagingCore {

    private MobileMessagingTestable(Context context, Broadcaster broadcaster, ExecutorService executorService, FirebaseAppProvider firebaseAppProvider) {
        super(context, broadcaster, executorService, new ModuleLoader(context), firebaseAppProvider);
    }

    public static MobileMessagingTestable create(Context context, Broadcaster broadcaster, MobileApiResourceProvider mobileApiResourceProvider, FirebaseAppProvider firebaseAppProvider) {
        MobileMessagingTestable instance = new MobileMessagingTestable(context, broadcaster, Executors.newSingleThreadExecutor(), firebaseAppProvider);
        Platform.reset(instance);
        MobileMessagingCore.mobileApiResourceProvider = mobileApiResourceProvider;
        return instance;
    }
}
