package org.infobip.mobile.messaging.chat.mobileapi

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.infobip.mobile.messaging.MobileMessaging
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.core.SessionStorage
import org.infobip.mobile.messaging.chat.core.widget.InstanceId
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError
import org.infobip.mobile.messaging.mobileapi.Result
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Synchronizes all in-app chat related data, such as widget configuration - [WidgetInfo] and livechat registration ID.
 */
internal class InAppChatSynchronizer(
    private val instanceId: InstanceId,
    private val sessionStorage: SessionStorage,
    private val propertyHelper: PropertyHelper,
    private val lcWidgetInfoSynchronizer: LivechatWidgetConfigSynchronizer,
    private val lcRegistrationChecker: LivechatRegistrationChecker,
) {

    companion object {
        private const val TAG = "InAppChatSynchronizer"
        private val isSyncInProgress: AtomicBoolean = AtomicBoolean(false)
    }

    constructor(context: Context, instanceId: InstanceId) : this(
        instanceId = instanceId,
        sessionStorage = SessionStorage,
        propertyHelper = PropertyHelper(context),
        lcWidgetInfoSynchronizer = LivechatWidgetConfigSynchronizer(context),
        lcRegistrationChecker = LivechatRegistrationChecker(context),
    )

    @JvmOverloads
    fun sync(
        pushRegistrationId: String?,
        delay: Long = 0L
    ) {
        if (isSyncInProgress.get()) {
            MobileMessagingLogger.d(TAG, "In-app chat sync from $instanceId skipped. Another sync is in progress.")
            return
        }
        if (pushRegistrationId.isNullOrBlank()) {
            MobileMessagingLogger.d(LivechatWidgetConfigSynchronizer.TAG, "In-app chat sync from $instanceId skipped. Push registration ID is not available yet.")
            return
        }
        val syncResult: Result<WidgetInfo, MobileMessagingError>? = sessionStorage.lcWidgetConfigSyncResult
        val isChatActivated = propertyHelper.findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_ACTIVATED)
        if (isChatActivated && (syncResult == null || syncResult.data == null)) {
            isSyncInProgress.set(true)
            MobileMessagingLogger.d(TAG, "In-app chat sync started from $instanceId")
            val syncAction = {
                lcWidgetInfoSynchronizer.getWidgetConfiguration(
                    object : MobileMessaging.ResultListener<WidgetInfo>() {
                        override fun onResult(result: Result<WidgetInfo, MobileMessagingError>?) {
                            sessionStorage.lcWidgetConfigSyncResult = result
                            lcRegistrationChecker.sync(
                                widgetId = result?.data?.id,
                                pushRegistrationId = pushRegistrationId,
                                callsEnabled = result?.data?.isCallsEnabled
                            )
                            isSyncInProgress.set(false)
                        }
                    }
                )
            }

            if (delay > 0L) {
                Handler(Looper.getMainLooper()).postDelayed({
                    syncAction()
                }, delay)
            } else {
                syncAction()
            }

        } else {
            MobileMessagingLogger.d(TAG, "In-app chat sync from $instanceId skipped. In-app chat is not activated or it is already synced.")
            isSyncInProgress.set(false)
        }
    }

}