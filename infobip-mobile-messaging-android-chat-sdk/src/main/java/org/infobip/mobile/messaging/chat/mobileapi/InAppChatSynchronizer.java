package org.infobip.mobile.messaging.chat.mobileapi;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.chat.MobileApiChat;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcaster;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.HashSet;
import java.util.List;


public class InAppChatSynchronizer {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final AndroidBroadcaster coreBroadcaster;
    private final InAppChatBroadcaster inAppChatBroadcaster;
    private final MobileApiChat mobileApiChat;
    private final MRetryPolicy retryPolicy;

    public InAppChatSynchronizer(Context context,
                                 MobileMessagingCore mobileMessagingCore,
                                 AndroidBroadcaster coreBroadcaster,
                                 InAppChatBroadcaster inAppChatBroadcaster,
                                 MobileApiChat mobileApiChat) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.coreBroadcaster = coreBroadcaster;
        this.inAppChatBroadcaster = inAppChatBroadcaster;
        this.mobileApiChat = mobileApiChat;
        this.retryPolicy = new RetryPolicyProvider(context).DEFAULT();
    }

    public void getWidgetConfiguration(final MobileMessaging.ResultListener<WidgetInfo> listener) {
        if (!mobileMessagingCore.isRegistrationAvailable()) {
            if (listener != null) {
                listener.onResult(new Result<WidgetInfo, MobileMessagingError>(InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        if (mobileMessagingCore.isDepersonalizeInProgress()) {
            MobileMessagingLogger.w("Depersonalization is in progress, will report custom event later");
            if (listener != null) {
                listener.onResult(new Result<WidgetInfo, MobileMessagingError>(InternalSdkError.DEPERSONALIZATION_IN_PROGRESS.getError()));
            }
            return;
        }

        new MRetryableTask<Void, WidgetInfo>() {

            @Override
            public WidgetInfo run(Void[] voids) {
                MobileMessagingLogger.v("GET WIDGET CONFIGURATION >>>");
                return mobileApiChat.getWidgetConfiguration();
            }

            @Override
            public void after(WidgetInfo widgetInfo) {
                MobileMessagingLogger.v("GET WIDGET CONFIGURATION DONE <<<");
                PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.getKey(), widgetInfo.getId());
                PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.getKey(), widgetInfo.getTitle());
                PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.getKey(), widgetInfo.getPrimaryColor());
                PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.getKey(), widgetInfo.getBackgroundColor());
                PreferenceHelper.saveLong(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE.getKey(), widgetInfo.getMaxUploadContentSize());
                PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTITHREAD.getKey(), widgetInfo.isMultiThread());
                PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTICHANNEL_CONVERSATION.getKey(), widgetInfo.isMultiChannelConversationEnabled());
                PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_ENABLED.getKey(), widgetInfo.isCallsEnabled());
                List<String> themes = widgetInfo.getThemeNames();
                if (themes != null) {
                    PreferenceHelper.saveStringSet(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_THEMES.getKey(), new HashSet<String>(themes));
                }
                //just cleanup, remove it in next version
                PreferenceHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_AVAILABLE.getKey());
                if (listener != null) {
                    listener.onResult(new Result<>(widgetInfo));
                }
                inAppChatBroadcaster.chatConfigurationSynced();
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("GET WIDGET CONFIGURATION ERROR <<<", error);

                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);

                if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
                }

                coreBroadcaster.error(MobileMessagingError.createFrom(error));
                if (listener != null) {
                    listener.onResult(new Result<WidgetInfo, MobileMessagingError>(MobileMessagingError.createFrom(error)));
                }
            }
        }
                .retryWith(retryPolicy)
                .execute();
    }
}