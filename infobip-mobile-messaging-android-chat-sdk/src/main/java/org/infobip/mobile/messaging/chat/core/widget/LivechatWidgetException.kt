package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.chat.core.InAppChatException

class LivechatWidgetException(
    message: String? = null,
    code: Int? = null,
    name: String? = null,
    origin: String? = null,
    platform: String? = null,
) : InAppChatException(message, code, name, origin, platform) {

    companion object {
        private val serializer = JsonSerializer(false)

        internal fun parse(json: String, method: LivechatWidgetMethod? = null): LivechatWidgetException {
            var exception = runCatching {
                serializer.deserialize(json, LivechatWidgetException::class.java)
            }.getOrNull() ?: LivechatWidgetException()

            //JSON like "{}" is parsed without error but exception is empty, this make sure we have a valid message and origin
            if (exception.message.isNullOrBlank()) {
                val message = if (method != null) {
                    "${method.name}() $json"
                } else {
                    json
                }
                exception = exception.copy(message = message)
            }

            if (exception.origin.isNullOrBlank()) {
                exception = exception.copy(origin = ORIGIN_LIVECHAT)
            }

            return exception
        }

        fun fromAndroid(message: String): LivechatWidgetException {
            return LivechatWidgetException(
                message = message,
                origin = ORIGIN_ANDROID_SDK,
                platform = PLATFORM_ANDROID
            )
        }
    }

    private fun copy(
        message: String? = this.message,
        code: Int? = this.code,
        name: String? = this.name,
        origin: String? = this.origin,
        platform: String? = this.platform,
    ) : LivechatWidgetException {
        return LivechatWidgetException(message, code, name, origin, platform)
    }

}