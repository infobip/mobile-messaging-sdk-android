package org.infobip.mobile.messaging.cloud;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Lazy;

public class MobileMessagingCloudHandler {

    private static final String PREFIX = "org.infobip.mobile.messaging.cloud.";

    static final String EXTRA_SENDER_ID = PREFIX + "SENDER_ID";
    static final String EXTRA_TOKEN = PREFIX + "TOKEN";

    static final String ACTION_CLOUD_MESSAGE_RECEIVE = PREFIX + "MESSAGE_RECEIVE";
    static final String ACTION_TOKEN_ACQUIRE = PREFIX + "TOKEN_ACQUIRE";
    static final String ACTION_TOKEN_CLEANUP = PREFIX + "TOKEN_CLEANUP";
    static final String ACTION_TOKEN_RESET = PREFIX + "TOKEN_RESET";
    static final String ACTION_NEW_TOKEN = PREFIX + "NEW_TOKEN";

    private final Lazy<RegistrationTokenHandler, Context> registrationTokenHandler;
    private final Lazy<MobileMessageHandler, Context> mobileMessageHandler;

    public MobileMessagingCloudHandler(RegistrationTokenHandler registrationTokenHandler, MobileMessageHandler mobileMessageHandler) {
        this.registrationTokenHandler = Lazy.just(registrationTokenHandler);
        this.mobileMessageHandler = Lazy.just(mobileMessageHandler);
    }

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
                handleTokenCleanup(context, intent);
                break;

            case ACTION_TOKEN_RESET:
                handleTokenReset(context, intent);
                break;

            case ACTION_TOKEN_ACQUIRE:
                handleTokenAcquire(context, intent);
                break;
        }
    }

    private void handleNewToken(Context context, @NonNull Intent intent) {
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);
        String token = intent.getStringExtra(EXTRA_TOKEN);
        registrationTokenHandler.get(context).handleNewToken(senderId, token);
    }

    private void handleTokenAcquire(Context context, @NonNull Intent intent) {
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);
        registrationTokenHandler.get(context).acquireNewToken(senderId);
    }

    private void handleTokenCleanup(Context context, @NonNull Intent intent) {
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);
        registrationTokenHandler.get(context).cleanupToken(senderId);
    }

    private void handleTokenReset(Context context, @NonNull Intent intent) {
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);
        RegistrationTokenHandler handler = registrationTokenHandler.get(context);
        handler.cleanupToken(senderId);
        handler.acquireNewToken(senderId);
    }

    private void handleMessage(Context context, @NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            MobileMessagingLogger.e("No extras in intent, cannot receive message");
            return;
        }

        Message message = MessageBundleMapper.messageFromBundle(extras);
        mobileMessageHandler.get(context).handleMessage(message);
    }
}
