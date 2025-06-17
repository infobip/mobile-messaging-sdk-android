package org.infobip.mobile.messaging.chat.mobileapi

import android.content.Context
import org.infobip.mobile.messaging.MobileMessaging
import org.infobip.mobile.messaging.MobileMessagingCore
import org.infobip.mobile.messaging.api.chat.MobileApiChat
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcaster
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcasterImpl
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.InternalSdkError
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError
import org.infobip.mobile.messaging.mobileapi.Result
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterException
import org.infobip.mobile.messaging.platform.AndroidBroadcaster
import org.infobip.mobile.messaging.util.PreferenceHelper

/**
 * Synchronizes Livechat widget configuration - [WidgetInfo].
 */
internal class LivechatWidgetConfigSynchronizer(
    private val context: Context,
    private val mmCore: MobileMessagingCore,
    private val coreBroadcaster: AndroidBroadcaster,
    private val chatBroadcaster: InAppChatBroadcaster,
    private val mobileApiChat: MobileApiChat,
) {
    companion object {
        const val TAG = "LcWidgetConfigSynchronizer"
    }

    constructor(context: Context) : this(
        context,
        MobileMessagingCore.getInstance(context),
        AndroidBroadcaster(context),
        InAppChatBroadcasterImpl(context),
        MobileApiResourceProvider().getMobileApiChat(context),
    )

    private val retryPolicy: MRetryPolicy = RetryPolicyProvider(context).DEFAULT()

    @JvmOverloads
    fun getWidgetConfiguration(listener: MobileMessaging.ResultListener<WidgetInfo>? = null) {
        if (!mmCore.isRegistrationAvailable()) {
            MobileMessagingLogger.d(TAG, "Livechat widget configuration sync skipped. Push registration ID is not available yet.")
            listener?.onResult(Result(InternalSdkError.NO_VALID_REGISTRATION.error))
            return
        }

        if (mmCore.isDepersonalizeInProgress()) {
            MobileMessagingLogger.d(TAG, "Livechat widget configuration sync skipped. Depersonalization is in progress.")
            listener?.onResult(Result(InternalSdkError.DEPERSONALIZATION_IN_PROGRESS.error))
            return
        }

        object : MRetryableTask<Void, WidgetInfo>() {

            override fun run(ins: Array<out Void>?): WidgetInfo {
                MobileMessagingLogger.v("GET WIDGET CONFIGURATION >>>")
                return mobileApiChat.getWidgetConfiguration()
            }

            override fun after(widgetInfo: WidgetInfo?) {
                widgetInfo?.let {
                    MobileMessagingLogger.v("GET WIDGET CONFIGURATION DONE <<<")
                    PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.key, widgetInfo.getId())
                    PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.key, widgetInfo.getTitle())
                    PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.key, widgetInfo.getPrimaryColor())
                    PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.key, widgetInfo.getBackgroundColor())
                    PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_TEXT_COLOR.key, widgetInfo.getPrimaryTextColor())
                    PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTITHREAD.key, widgetInfo.isMultiThread())
                    PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTICHANNEL_CONVERSATION.key, widgetInfo.isMultiChannelConversationEnabled())
                    PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_ENABLED.key, widgetInfo.isCallsEnabled())
                    widgetInfo.getThemeNames()?.let { themes ->
                        PreferenceHelper.saveStringSet(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_THEMES.key, HashSet(themes))
                    }
                    widgetInfo.getAttachmentConfig()?.let { attachmentConfig ->
                        PreferenceHelper.saveLong(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ATTACHMENT_MAX_SIZE.key, attachmentConfig.maxSize)
                        PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ATTACHMENT_ENABLED.key, attachmentConfig.isEnabled)
                        if (attachmentConfig.allowedExtensions != null) {
                            PreferenceHelper.saveStringSet(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ATTACHMENT_ALLOWED_EXTENSIONS.key, attachmentConfig.allowedExtensions)
                        }
                    }
                    listener?.onResult(Result(widgetInfo))
                    chatBroadcaster.chatConfigurationSynced()
                    chatBroadcaster.chatAvailabilityUpdated(IsChatAvailable.check(widgetInfo.getId(), mmCore))
                }
            }

            override fun error(error: Throwable?) {
                MobileMessagingLogger.v("GET WIDGET CONFIGURATION ERROR <<<", error)

                val mobileMessagingError = MobileMessagingError.createFrom(error)
                if (error is BackendInvalidParameterException) {
                    mmCore.handleNoRegistrationError(mobileMessagingError)
                }

                listener?.onResult(Result(MobileMessagingError.createFrom(error)))
                coreBroadcaster.error(MobileMessagingError.createFrom(error))
                chatBroadcaster.chatAvailabilityUpdated(false)
            }

        }
            .retryWith(retryPolicy)
            .execute()
    }

}