/*
 * MobileMessagingCloudHandler.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.cloud;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.dal.data.MessageDataMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Lazy;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.work.Data;

public class MobileMessagingCloudHandler {

    private static final String PREFIX = "org.infobip.mobile.messaging.cloud.";

    static final String EXTRA_TOKEN = PREFIX + "TOKEN";

    static final String ACTION_CLOUD_MESSAGE_RECEIVE = PREFIX + "MESSAGE_RECEIVE";
    static final String ACTION_TOKEN_ACQUIRE = PREFIX + "TOKEN_ACQUIRE";
    static final String ACTION_TOKEN_CLEANUP = PREFIX + "TOKEN_CLEANUP";
    static final String ACTION_TOKEN_RESET = PREFIX + "TOKEN_RESET";
    static final String ACTION_NEW_TOKEN = PREFIX + "NEW_TOKEN";
    static final String MM_ACTION = PREFIX + "ACTION";

    private final Lazy<RegistrationTokenHandler, Context> registrationTokenHandler;
    private final Lazy<MobileMessageHandler, Context> mobileMessageHandler;

    public MobileMessagingCloudHandler(RegistrationTokenHandler registrationTokenHandler, MobileMessageHandler mobileMessageHandler) {
        this.registrationTokenHandler = Lazy.just(registrationTokenHandler);
        this.mobileMessageHandler = Lazy.just(mobileMessageHandler);
    }

    public void handleWork(Context context, Data data) {
        String action = data.getString(MM_ACTION);
        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_CLOUD_MESSAGE_RECEIVE:
                handleMessage(context, data);
                break;

            case ACTION_NEW_TOKEN:
                handleNewToken(context, data);
                break;

            case ACTION_TOKEN_CLEANUP:
                handleTokenCleanup(context);
                break;

            case ACTION_TOKEN_RESET:
                handleTokenReset(context);
                break;

            case ACTION_TOKEN_ACQUIRE:
                handleTokenAcquire(context);
                break;
        }
    }

    private void handleNewToken(Context context, Data data) {
        String token = data.getString(EXTRA_TOKEN);
        registrationTokenHandler.get(context).handleNewToken(token);
    }

    private void handleTokenAcquire(Context context) {
        registrationTokenHandler.get(context).acquireNewToken();
    }

    private void handleTokenCleanup(Context context) {
        registrationTokenHandler.get(context).cleanupToken();
    }

    private void handleTokenReset(Context context) {
        registrationTokenHandler.get(context).reissueToken();
    }

    private void handleMessage(Context context, Data data) {
        String extra = data.getString(ACTION_CLOUD_MESSAGE_RECEIVE);
        if (extra == null) {
            MobileMessagingLogger.w("No extras in data, cannot receive message");
            return;
        }
        Message message = MessageDataMapper.messageFromString(extra);
        mobileMessageHandler.get(context).handleMessage(message);
    }

    // Used for pre-Oreo versions
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void handleWork(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_CLOUD_MESSAGE_RECEIVE:
                handleMessage(context, intent);
                break;

            case ACTION_NEW_TOKEN:
                handleNewToken(context, intent);
                break;

            case ACTION_TOKEN_CLEANUP:
                handleTokenCleanup(context);
                break;

            case ACTION_TOKEN_RESET:
                handleTokenReset(context);
                break;

            case ACTION_TOKEN_ACQUIRE:
                handleTokenAcquire(context);
                break;
        }
    }

    private void handleNewToken(Context context, @NonNull Intent intent) {
        String token = intent.getStringExtra(EXTRA_TOKEN);
        registrationTokenHandler.get(context).handleNewToken(token);
    }

    private void handleMessage(Context context, @NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            MobileMessagingLogger.w("No extras in intent, cannot receive message");
            return;
        }

        Message message = MessageBundleMapper.messageFromBundle(extras);
        mobileMessageHandler.get(context).handleMessage(message);
    }
}
