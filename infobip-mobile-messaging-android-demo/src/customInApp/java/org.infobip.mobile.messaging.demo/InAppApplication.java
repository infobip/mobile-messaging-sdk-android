/*
 * InAppApplication.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import androidx.core.content.ContextCompat;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;

public class InAppApplication extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        new MobileMessaging.Builder(this)
                .withMessageStore(SQLiteMessageStore.class)
                .withDisplayNotification(new NotificationSettings.Builder(this)
                        .withMultipleNotifications()
                        .withDefaultIcon(R.drawable.ic_notification)
                        .withColor(ContextCompat.getColor(this, R.color.red))
                        .withoutModalInAppNotifications()
                        .build())
                .build();
    }
}
