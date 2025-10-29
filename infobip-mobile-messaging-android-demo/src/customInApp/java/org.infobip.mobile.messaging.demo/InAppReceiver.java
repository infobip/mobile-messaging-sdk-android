/*
 * InAppReceiver.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

public class InAppReceiver extends BroadcastReceiver {
    final static String TAG = "InAppReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            MobileMessagingLogger.w(TAG, "Can't get extras from Intent");
            return;
        }

        Message message = Message.createFrom(intent.getExtras());
        if (message == null) {
            MobileMessagingLogger.w(TAG, "Can't get message from Intent");
            return;
        }
        MobileInteractive.getInstance(context).displayInAppDialogFor(message);
    }
}
