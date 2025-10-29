/*
 * ChatWithCallsApplication.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.widget.Toast;

import com.infobip.webrtc.ui.InfobipRtcUi;

public class ChatWithCallsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        new InfobipRtcUi.Builder(this)
                .withInAppChatCalls(
                        () -> Toast.makeText(this, "Calls registration successful!", Toast.LENGTH_SHORT).show(),
                        throwable -> Toast.makeText(this, "Calls registration failed: " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                )
                .build();

    }
}