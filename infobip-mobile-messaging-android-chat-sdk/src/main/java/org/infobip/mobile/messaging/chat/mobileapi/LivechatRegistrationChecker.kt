package org.infobip.mobile.messaging.chat.mobileapi

import android.content.Context
import org.infobip.mobile.messaging.MobileMessagingCore
import org.infobip.mobile.messaging.api.appinstance.LivechatDestination
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcaster
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcasterImpl
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider
import org.infobip.mobile.messaging.mobileapi.common.MAsyncTask
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class LivechatRegistrationChecker(
    private val context: Context,
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
    }

    constructor(context: Context) : this(
        context,
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
            MobileMessagingLogger.d("LivechatRegistration check skipped. Another check of livechat registration id is progress.")
            return
        }
        val enableCalls = callsEnabled ?: propertyHelper.findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_ENABLED)
        if (!enableCalls){
            MobileMessagingLogger.d("LivechatRegistration check skipped. Call feature is disabled.")
            return
        }
        isSyncInProgress = true

        object : MAsyncTask<Void, String?>() {

            override fun run(ins: Array<out Void>?): String? {
                MobileMessagingLogger.v("CHECK LIVECHAT REGISTRATION >>>")
                val pushRegIg = pushRegistrationId ?: mmCore.pushRegistrationId
                require(pushRegIg?.isNotBlank() == true) { "Cannot obtain livechatRegistrationId. Missing pushRegistrationId argument." }
                val destinations: Array<out LivechatDestination>? = mobileApiAppInstance.getLivechatContactInformation(pushRegIg)?.getLiveChatDestinations()
                return when (destinations?.size) {
                    null, 0 -> {
                        MobileMessagingLogger.d("Livechat contact information array is null or empty.")
                        null
                    }
                    1 -> destinations.first().getRegistrationId()
                    else -> {
                        val wId = widgetId ?: propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID)
                        require(wId?.isNotBlank() == true) { "Cannot obtain livechatRegistrationId. Missing widgetId argument." }
                        val widget = destinations.firstOrNull { it.getWidgetId() == wId }
                        if (widget == null) {
                            MobileMessagingLogger.d("Livechat contact information for widget id = $wId does not exits.")
                            null
                        } else {
                            widget.getRegistrationId()
                        }
                    }
                }
            }

            override fun after(livechatRegistrationId: String?) {
                MobileMessagingLogger.v("CHECK LIVECHAT REGISTRATION DONE <<<")
                if (livechatRegistrationId?.isNotBlank() == true && livechatRegistrationId != reportedRegistrationId) {
                    inAppChatBroadcaster.livechatRegistrationIdUpdated(livechatRegistrationId)
                    reportedRegistrationId = livechatRegistrationId
                    MobileMessagingLogger.v("Livechat registration id = $livechatRegistrationId broadcast sent.")
                } else {
                    MobileMessagingLogger.v("Livechat registration id = $livechatRegistrationId broadcast skipped.")
                }
                isSyncInProgress = false
            }

            override fun error(error: Throwable?) {
                MobileMessagingLogger.e("CHECK LIVECHAT REGISTRATION ERROR <<<", error)
                isSyncInProgress = false
            }

        }.execute(executor)

    }

}