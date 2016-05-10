package org.infobip.mobile.messaging.telephony;

import android.content.Context;

import org.infobip.mobile.messaging.tasks.MobileApiResourceProvider;

/**
 * @author sslavin
 * @since 22.04.2016.
 */
public class MobileNetworkChangeHandler {

    private Context context;

    public MobileNetworkChangeHandler(Context context) {
        this.context = context;
    }

    void handleNetworkStateChange() {
        MobileNetworkInfo newInfo = MobileNetworkInfo.fromSystem(context);
        MobileNetworkInfo oldInfo = MobileNetworkInfo.fromProperties(context);
        if (!oldInfo.isEqual(newInfo)) {
            newInfo.save();
            MobileApiResourceProvider.INSTANCE.resetMobileApi();
        }
    }
}
