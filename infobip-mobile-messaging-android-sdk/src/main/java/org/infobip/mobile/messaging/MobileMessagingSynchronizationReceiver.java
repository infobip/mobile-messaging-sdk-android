/*
 * MobileMessagingSynchronizationReceiver.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * @author sslavin
 * @since 31/05/2017.
 */

public class MobileMessagingSynchronizationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (LocalEvent.APPLICATION_FOREGROUND.getKey().equals(intent.getAction())) {
            MobileMessagingCore.getInstance(context.getApplicationContext()).foregroundSync();
        }
    }
}
