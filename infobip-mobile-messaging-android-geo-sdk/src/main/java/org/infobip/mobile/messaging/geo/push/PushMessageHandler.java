package org.infobip.mobile.messaging.geo.push;


import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.storage.MessageStore;

public class PushMessageHandler {

    private static final String TAG = "Geofencing";

    public void handleGeoMessage(Context context, Message message) {
        GeofencingHelper geofencingHelper = new GeofencingHelper(context);
        MessageStore messageStoreForGeo = geofencingHelper.getMessageStoreForGeo();

        try {
            messageStoreForGeo.save(context, message);

            GeofencingHelper.setAllActiveGeoAreasMonitored(context, false);
            geofencingHelper.startGeoMonitoringIfNecessary();

        } catch (Exception e) {
            MobileMessagingLogger.e(TAG, InternalSdkError.ERROR_SAVING_MESSAGE.get(), e);
        }
    }
}
