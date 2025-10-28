package org.infobip.mobile.messaging.chat.mobileapi

import android.content.Context
import org.infobip.mobile.messaging.MobileMessagingCore
import org.infobip.mobile.messaging.api.appinstance.LivechatDestination
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcaster
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcasterImpl
import org.infobip.mobile.messaging.chat.core.InAppChatException
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider
import org.infobip.mobile.messaging.mobileapi.common.MAsyncTask
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class LivechatRegistrationChecker(
    private val mmCore: MobileMessagingCore,
    private val propertyHelper: PropertyHelper,
    private val inAppChatBroadcaster: InAppChatBroadcaster,
    private val mobileApiAppInstance: MobileApiAppInstance,
    private val executor: Executor,
) {

    companion object {
        //used as session cache to avoid sending broadcast with same registration id in a row
        private var reportedRegistrationId: String? = null
        private var isSyncInProgress: Boolean = false
        const val TAG = "LcRegistrationChecker"
    }

    constructor(context: Context) : this(
        MobileMessagingCore.getInstance(context),
        PropertyHelper(context),
        InAppChatBroadcasterImpl(context),
        MobileApiResourceProvider().getMobileApiAppInstance(context),
        Executors.newSingleThreadExecutor()
    )

    @JvmOverloads
    fun sync(
        widgetId: String? = null,
        pushRegistrationId: String? = null,
        callsEnabled: Boolean? = null,
    ) {
        if (isSyncInProgress) {
            MobileMessagingLogger.d(TAG,"LivechatRegistration check skipped. Another check is in progress.")
            return
        }
        val enableCalls = callsEnabled ?: propertyHelper.findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_ENABLED)
        if (!enableCalls) {
            MobileMessagingLogger.d(TAG,"LivechatRegistration check skipped. Call feature is disabled.")
            return
        }
        isSyncInProgress = true

        object : MAsyncTask<Void, String?>() {

            override fun run(ins: Array<out Void>?): String? {
                MobileMessagingLogger.v(TAG,"CHECK LIVECHAT REGISTRATION >>>")
                val pushRegIg = pushRegistrationId ?: mmCore.pushRegistrationId
                if (pushRegIg.isNullOrBlank()) {
                    throw InAppChatException.MissingPushRegistrationId()
                }
                val destinations: Array<out LivechatDestination>? = mobileApiAppInstance.getLivechatContactInformation(pushRegIg)?.liveChatDestinations
                return when (destinations?.size) {
                    null, 0 -> {
                        MobileMessagingLogger.d(TAG,"Livechat contact information array is null or empty.")
                        null
                    }
                    1 -> destinations.first().registrationId
                    else -> {
                        val wId = widgetId ?: propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID)
                        if (wId.isNullOrBlank()) {
                            throw InAppChatException.MissingLivechatWidgetId()
                        }
                        val widget = destinations.firstOrNull { it.widgetId == wId }
                        if (widget == null) {
                            MobileMessagingLogger.d(TAG,"Livechat contact information for widget id = $wId does not exits.")
                            null
                        } else {
                            widget.registrationId
                        }
                    }
                }
            }

            override fun after(livechatRegistrationId: String?) {
                MobileMessagingLogger.v(TAG,"CHECK LIVECHAT REGISTRATION DONE <<<")
                if (livechatRegistrationId?.isNotBlank() == true && livechatRegistrationId != reportedRegistrationId) {
                    inAppChatBroadcaster.livechatRegistrationIdUpdated(livechatRegistrationId)
                    reportedRegistrationId = livechatRegistrationId
                    MobileMessagingLogger.d(TAG,"Livechat registration id = $livechatRegistrationId broadcast sent.")
                } else {
                    MobileMessagingLogger.d(TAG,"Livechat registration id = $livechatRegistrationId broadcast skipped.")
                }
                isSyncInProgress = false
            }

            override fun error(error: Throwable?) {
                MobileMessagingLogger.e(TAG,"CHECK LIVECHAT REGISTRATION ERROR <<<", error)
                isSyncInProgress = false
            }

        }.execute(executor)

    }

}