package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

internal data class LivechatWidgetError(
    val message: String? = null,
    val code: Int? = null
) {
    companion object {
        private val serializer = JsonSerializer(false)

        fun fromJson(json: String): LivechatWidgetError {
            return runCatching {
                serializer.deserialize(json, LivechatWidgetError::class.java)
            }.onFailure {
                MobileMessagingLogger.e("Error parsing error message: $json", it)
            }.getOrDefault(LivechatWidgetError(message = json))
        }
    }
}