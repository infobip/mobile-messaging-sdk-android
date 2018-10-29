package org.infobip.mobile.messaging.cloud;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.JobIntentService;
import android.text.TextUtils;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Lazy;
import org.infobip.mobile.messaging.platform.Platform;

import static org.infobip.mobile.messaging.MobileMessagingJob.CLOUD_INTENT_JOB_ID;
import static org.infobip.mobile.messaging.MobileMessagingJob.getScheduleId;

/**
 * @author sslavin
 * @since 03/09/2018.
 */
public class MobileMessagingCloudService extends JobIntentService {

    private static final String PREFIX = "org.infobip.mobile.messaging.cloud.";

    private static final String ACTION_CLOUD_MESSAGE_RECEIVE = PREFIX + "MESSAGE_RECEIVE";
    private static final String ACTION_TOKEN_ACQUIRE = PREFIX + "TOKEN_ACQUIRE";
    private static final String ACTION_TOKEN_CLEANUP = PREFIX + "TOKEN_CLEANUP";
    private static final String ACTION_TOKEN_RESET = PREFIX + "TOKEN_RESET";
    private static final String ACTION_NEW_TOKEN = PREFIX + "NEW_TOKEN";

    private static final String EXTRA_SENDER_ID = PREFIX + "SENDER_ID";
    private static final String EXTRA_TOKEN = PREFIX + "TOKEN";

    private final Lazy<RegistrationTokenHandler, Context> registrationTokenHandler;
    private final Lazy<MobileMessageHandler, Context> mobileMessageHandler;

    public MobileMessagingCloudService() {
        this.registrationTokenHandler = Platform.registrationTokenHandler;
        this.mobileMessageHandler = Platform.mobileMessageHandler;
    }

    @VisibleForTesting
    public MobileMessagingCloudService(RegistrationTokenHandler registrationTokenHandler, MobileMessageHandler mobileMessageHandler) {
        this.registrationTokenHandler = Lazy.just(registrationTokenHandler);
        this.mobileMessageHandler = Lazy.just(mobileMessageHandler);
    }

    /**
     * Convenience methods for enqueuing work in to this service.
     */

    public static void enqueueTokenAcquisition(Context context, String senderId) {
        if (TextUtils.isEmpty(senderId)) {
            MobileMessagingLogger.e("Cannot acquire token, senderId is empty");
            return;
        }

        enqueueWork(context, new Intent(ACTION_TOKEN_ACQUIRE)
                .putExtra(EXTRA_SENDER_ID, senderId));
    }

    public static void enqueueTokenCleanup(Context context, String senderId) {
        if (TextUtils.isEmpty(senderId)) {
            MobileMessagingLogger.e("Cannot cleanup token, senderId is empty");
            return;
        }

        enqueueWork(context, new Intent(ACTION_TOKEN_CLEANUP)
                .putExtra(EXTRA_SENDER_ID, senderId));
    }

    public static void enqueueTokenReset(Context context, String senderId) {
        if (TextUtils.isEmpty(senderId)) {
            MobileMessagingLogger.e("Cannot reset token, senderId is empty");
            return;
        }

        enqueueWork(context, new Intent(ACTION_TOKEN_RESET)
                .putExtra(EXTRA_SENDER_ID, senderId));
    }

    public static void enqueueNewToken(Context context, String senderId, String token) {
        if (TextUtils.isEmpty(senderId)) {
            MobileMessagingLogger.e("Cannot process new token, senderId is empty");
            return;
        }

        if (TextUtils.isEmpty(senderId)) {
            MobileMessagingLogger.e("Cannot process new token, token is empty");
            return;
        }

        enqueueWork(context, new Intent(MobileMessagingCloudService.ACTION_NEW_TOKEN)
                .putExtra(MobileMessagingCloudService.EXTRA_SENDER_ID, senderId)
                .putExtra(MobileMessagingCloudService.EXTRA_TOKEN, token));
    }

    public static void enqueueNewMessage(Context context, Message message) {
        Bundle messageBundle = MessageBundleMapper.messageToBundle(message);
        enqueueWork(context, new Intent(MobileMessagingCloudService.ACTION_CLOUD_MESSAGE_RECEIVE)
                .putExtras(messageBundle));
    }

    private static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MobileMessagingCloudService.class, getScheduleId(context, CLOUD_INTENT_JOB_ID), work.setPackage(context.getPackageName()));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_CLOUD_MESSAGE_RECEIVE:
                handleMessage(intent);
                break;

            case ACTION_NEW_TOKEN:
                handleNewToken(intent);
                break;

            case ACTION_TOKEN_CLEANUP:
                handleTokenCleanup(intent);
                break;

            case ACTION_TOKEN_RESET:
                handleTokenReset(intent);
                break;

            case ACTION_TOKEN_ACQUIRE:
                handleTokenAcquire(intent);
                break;
        }
    }

    private void handleNewToken(@NonNull Intent intent) {
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);
        String token = intent.getStringExtra(EXTRA_TOKEN);
        registrationTokenHandler.get(this).handleNewToken(senderId, token);
    }

    private void handleTokenAcquire(@NonNull Intent intent) {
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);
        registrationTokenHandler.get(this).acquireNewToken(senderId);
    }

    private void handleTokenCleanup(@NonNull Intent intent) {
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);
        registrationTokenHandler.get(this).cleanupToken(senderId);
    }

    private void handleTokenReset(@NonNull Intent intent) {
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);
        RegistrationTokenHandler handler = registrationTokenHandler.get(this);
        handler.cleanupToken(senderId);
        handler.acquireNewToken(senderId);
    }

    private void handleMessage(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            MobileMessagingLogger.e("No extras in intent, cannot receive message");
            return;
        }

        Message message = MessageBundleMapper.messageFromBundle(extras);
        mobileMessageHandler.get(this).handleMessage(message);
    }
}
