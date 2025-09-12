package org.infobip.mobile.messaging.chat.core

import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetException
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.json.JSONObject

open class InAppChatException(
    override val message: String? = null,
    val code: Int? = null,
    val name: String? = null,
    val origin: String? = ORIGIN_ANDROID_SDK,
    val platform: String? = PLATFORM_ANDROID,
) : RuntimeException(message) {

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine(super.toString())
        code?.let { stringBuilder.appendLine("\tcode=$it,") }
        name?.let { stringBuilder.appendLine("\tname=$it,") }
        origin.let { stringBuilder.appendLine("\torigin=$it,") }
        platform.let { stringBuilder.appendLine("\tplatform=$it") }
        return stringBuilder.toString()
    }

    companion object {
        const val PLATFORM_ANDROID = "Android"
        const val ORIGIN_LIVECHAT = "Livechat"
        const val ORIGIN_ANDROID_SDK = "Android SDK"

        @JvmField
        val NO_INTERNET_CONNECTION = LivechatWidgetException(
            message = "No internet connection.",
            code = 10_000,
            name = "NO_INTERNET_CONNECTION",
            origin = ORIGIN_ANDROID_SDK,
            platform = PLATFORM_ANDROID,
        )

    }

    fun toJSON(): JSONObject? {
        return try {
            JSONObject().apply {
                putOpt("message", message)
                putOpt("code", code)
                putOpt("name", name)
                putOpt("origin", origin)
                putOpt("platform", platform)
            }
        } catch (e: Exception) {
            MobileMessagingLogger.w("Cannot convert InAppChatException to JSON: ", e)
            null
        }
    }
}