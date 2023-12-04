package org.infobip.mobile.messaging.chat.core

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

data class InAppChatWidgetError(
    val message: String? = null,
    val code: Int? = null
) {
    companion object {
        private val serializer = JsonSerializer(false)

        fun fromJson(json: String): InAppChatWidgetError {
            return runCatching {
                serializer.deserialize(json, InAppChatWidgetError::class.java)
            }.onFailure {
                MobileMessagingLogger.e("Error parsing error message: $json", it)
            } .getOrDefault(InAppChatWidgetError(message = json))
        }
    }
}