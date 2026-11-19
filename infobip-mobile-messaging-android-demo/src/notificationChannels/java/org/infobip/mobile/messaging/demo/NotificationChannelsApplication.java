/*
 * NotificationChannelsApplication.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.util.Log;

import androidx.core.content.ContextCompat;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;

/**
 * Initializes the SDK with the notification channel scenario selected in {@link MainActivity}.
 */
public class NotificationChannelsApplication extends android.app.Application {

    static final String TAG = "ChannelTest";

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannelScenario scenario = NotificationChannelScenario.load(this);
        Log.d(TAG, "Initializing SDK with scenario " + scenario.name());

        MobileMessaging.Builder builder = new MobileMessaging.Builder(this)
                .withMessageStore(SQLiteMessageStore.class)
                .withDisplayNotification(new NotificationSettings.Builder(this)
                        .withMultipleNotifications()
                        .withDefaultIcon(R.drawable.ic_notification)
                        .withColor(ContextCompat.getColor(this, R.color.red))
                        .build());
        scenario.configure(builder);
        builder.build();
    }
}
