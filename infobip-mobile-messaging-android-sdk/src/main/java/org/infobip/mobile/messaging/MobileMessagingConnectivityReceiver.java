/*
 * MobileMessagingConnectivityReceiver.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import org.infobip.mobile.messaging.util.MobileNetworkInformation;

import androidx.annotation.VisibleForTesting;

/**
 * Connectivity receiver for pre-7.0 Nougat devices
 * @author tjuric
 * @since 31/08/2017.
 */
public class MobileMessagingConnectivityReceiver extends BroadcastReceiver {

    private MobileMessagingCore mobileMessagingCore;

    public MobileMessagingConnectivityReceiver() {
    }

    @VisibleForTesting
    public MobileMessagingConnectivityReceiver(MobileMessagingCore mobileMessagingCore) {
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.isEmpty(mobileMessagingCore(context).getApplicationCode())) {
            return;
        }

        if (intent == null || !ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            return;
        }

        if (isInternetConnected(intent, context)) {
            mobileMessagingCore(context).retrySyncOnNetworkAvailable();
        }
    }

    private boolean isInternetConnected(Intent intent, Context context) {
        Boolean isNetworkAvailable = MobileNetworkInformation.isNetworkAvailable(context);
        return (isNetworkAvailable != null && isNetworkAvailable) ||
                (intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
                        && !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
    }

    private MobileMessagingCore mobileMessagingCore(Context context) {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context);
        }
        return mobileMessagingCore;
    }
}
