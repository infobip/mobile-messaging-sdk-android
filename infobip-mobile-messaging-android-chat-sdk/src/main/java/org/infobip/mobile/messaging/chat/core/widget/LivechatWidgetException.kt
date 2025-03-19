package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer

class LivechatWidgetException(
    override val message: String? = null,
    val code: Int? = null,
    val name: String? = null,
    val origin: String? = null,
    val platform: String? = null,
) : RuntimeException(message) {

    companion object {
        private val serializer = JsonSerializer(false)
        const val PLATFORM_ANDROID = "Android"
        const val ORIGIN_LIVECHAT = "Livechat"
        const val ORIGIN_ANDROID_SDK = "Android SDK"

        fun parse(json: String): LivechatWidgetException {
            return runCatching {
                serializer.deserialize(json, LivechatWidgetException::class.java)
            }.getOrDefault(LivechatWidgetException(message = json, origin = ORIGIN_LIVECHAT))
        }

        fun fromAndroid(message: String): LivechatWidgetException {
            return LivechatWidgetException(
                message = message,
                origin = ORIGIN_ANDROID_SDK,
                platform = PLATFORM_ANDROID
            )
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine(super.toString())
        code?.let { stringBuilder.appendLine("\tcode=$it,") }
        name?.let { stringBuilder.appendLine("\tname=$it,") }
        origin?.let { stringBuilder.appendLine("\torigin=$it,") }
        platform?.let { stringBuilder.appendLine("\tplatform=$it") }
        return stringBuilder.toString()
    }

}