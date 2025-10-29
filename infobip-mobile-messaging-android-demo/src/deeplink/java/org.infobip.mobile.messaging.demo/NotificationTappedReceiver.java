/*
 * NotificationTappedReceiver.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.infobip.mobile.messaging.Message;
import org.json.JSONObject;

import static org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper.messageToBundle;

/**
 * @author sslavin
 * @since 13/11/2017.
 */

public class NotificationTappedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Message message = Message.createFrom(intent.getExtras());
        if (message == null) return;
        String deepLink = message.getDeeplink();
        if (deepLink.isEmpty()) return;

        Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW);
        deepLinkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        deepLinkIntent.setData(Uri.parse(deepLink));
        deepLinkIntent.putExtras(messageToBundle(message));
        if (deepLinkIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(deepLinkIntent);
        }
    }
}
