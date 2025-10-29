/*
 * MobileNetworkChangeHandler.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.telephony;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;

/**
 * @author sslavin
 * @since 22.04.2016.
 */
class MobileNetworkChangeHandler {

    private final Context context;

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
