/*
 * MobileNetworkStateListener.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.telephony;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

/**
 * Created by sslavin on 22/04/16.
 */
public class MobileNetworkStateListener extends PhoneStateListener {

    private final MobileNetworkChangeHandler mobileNetworkChangeHandler;

    public MobileNetworkStateListener(Context context) {
        mobileNetworkChangeHandler = new MobileNetworkChangeHandler(context);
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(this, LISTEN_SERVICE_STATE);
        }
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        mobileNetworkChangeHandler.handleNetworkStateChange();
    }
}
