package org.infobip.mobile.messaging.telephony;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;

/**
 * @author sslavin
 * @since 22.04.2016.
 */
class MobileNetworkChangeHandler {

    private Context context;

    MobileNetworkChangeHandler(Context context) {
        this.context = context;
    }

    void handleNetworkStateChange() {
        MobileNetworkInfo newInfo = MobileNetworkInfo.fromSystem(context);
        MobileNetworkInfo oldInfo = MobileNetworkInfo.fromProperties(context);
        if (!oldInfo.isEqual(newInfo)) {
            newInfo.save();
            MobileMessagingCore.resetMobileApi();
        }
    }
}
