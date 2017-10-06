package org.infobip.mobile.messaging;

import android.content.Context;

import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.util.ModuleLoader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author sslavin
 * @since 13/03/2017.
 */

public class MobileMessagingTestable extends MobileMessagingCore {

    private MobileMessagingTestable(Context context, Broadcaster broadcaster, ExecutorService executorService) {
        super(context, broadcaster, executorService, new ModuleLoader(context));
    }

    public static MobileMessagingTestable create(Context context, Broadcaster broadcaster, MobileApiResourceProvider mobileApiResourceProvider) {
        MobileMessagingTestable instance = new MobileMessagingTestable(context, broadcaster, Executors.newSingleThreadExecutor());
        MobileMessagingCore.instance = instance;
        MobileMessagingCore.mobileApiResourceProvider = mobileApiResourceProvider;
        return instance;
    }
}
