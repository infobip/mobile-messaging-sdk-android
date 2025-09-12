package org.infobip.mobile.messaging.chat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.models.ContextualData
import org.infobip.mobile.messaging.chat.view.InAppChatErrorsHandler
import org.infobip.mobile.messaging.chat.view.InAppChatEventsListener
import org.infobip.mobile.messaging.chat.view.styles.InAppChatTheme
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError
import org.infobip.mobile.messaging.mobileapi.Result


/**
 * Session storage stores InAppChat data only during SDK lifetime.
 *
 * It is internal class and used to store data that are needed to access in multiple places in the Chat SDK.
 */
internal object SessionStorage {

    @get:Synchronized
    @set:Synchronized
    var domain: String? = null

    @get:Synchronized
    @set:Synchronized
    var widgetTheme: String? = null

    @get:Synchronized
    @set:Synchronized
    var theme: InAppChatTheme? = null

    @get:Synchronized
    @set:Synchronized
    var jwtProvider: JwtProvider? = null

    @get:Synchronized
    @set:Synchronized
    var contextualData: ContextualData? = null

    @get:Synchronized
    @set:Synchronized
    var lcWidgetConfigSyncResult: Result<WidgetInfo, MobileMessagingError>? = null

    @get:Synchronized
    @set:Synchronized
    var inAppChatEventsListener: InAppChatEventsListener? = null

    @get:Synchronized
    @set:Synchronized
    var inAppChatErrorsHandler: InAppChatErrorsHandler? = null

    @get:Synchronized
    @set:Synchronized
    var inAppChatNotificationInteractionHandler: InAppChatNotificationInteractionHandler? = null

    /**
     * InAppChat SDK coroutine scope
     */
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun clean() {
        domain = null
        widgetTheme = null
        theme = null
        jwtProvider = null
        contextualData = null
        lcWidgetConfigSyncResult = null
        inAppChatEventsListener = null
        inAppChatErrorsHandler = null
        inAppChatNotificationInteractionHandler = null
        scope.coroutineContext.cancelChildren()
    }

}