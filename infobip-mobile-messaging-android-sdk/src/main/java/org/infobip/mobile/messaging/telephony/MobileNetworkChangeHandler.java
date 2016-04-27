package org.infobip.mobile.messaging.telephony;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;

/**
 * Created by sslavin on 22/04/16.
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
            MobileMessaging.getInstance(context).reportUnreportedRegistration();
            // TODO: update user-agent
        }
    }
}
