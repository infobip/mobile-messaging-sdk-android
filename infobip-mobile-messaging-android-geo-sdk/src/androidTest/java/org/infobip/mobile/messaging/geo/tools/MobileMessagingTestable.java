package org.infobip.mobile.messaging.geo.tools;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.platform.Broadcaster;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author sslavin
 * @since 13/03/2017.
 */

public class MobileMessagingTestable extends MobileMessagingCore {

    private MobileMessagingTestable(Context context, Broadcaster broadcaster, ExecutorService executorService) {
        super(context, broadcaster, executorService);
    }

    public static MobileMessagingTestable create(Context context, Broadcaster broadcaster) {
        MobileMessagingTestable instance = new MobileMessagingTestable(context, broadcaster, Executors.newSingleThreadExecutor());
        MobileMessagingCore.instance = instance;
        return instance;
    }
}
