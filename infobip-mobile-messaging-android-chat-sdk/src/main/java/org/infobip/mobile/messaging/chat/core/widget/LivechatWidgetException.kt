/*
 * LivechatWidgetException.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.chat.core.InAppChatException

/**
 * Exception for Livechat Widget errors.
 *
 * This exception class extends [InAppChatException] and is specifically used for errors originated in Livechat Widget.
 *
 * @property message Human-readable error message
 * @property code Numeric error code for programmatic identification
 * @property name Machine-readable error name/identifier
 * @property origin The origin/source of the error (widget, SDK, etc.)
 * @property platform The platform where the error occurred (typically "Android")
 * @property technicalMessage Detailed technical information for debugging purposes
 *
 * @see InAppChatException for the base exception class
 */
open class LivechatWidgetException @JvmOverloads constructor(
    message: String? = null,
    code: Int? = null,
    name: String? = null,
    origin: String? = null,
    platform: String? = null,
) : InAppChatException(
    message = message,
    code = code,
    name = name,
    origin = origin,
    platform = platform,
) {

    companion object {
        private val serializer = JsonSerializer(false)

        /**
         * Parses a JSON string into a LivechatWidgetException.
         *
         * This method is used to deserialize widget errors received from the WebView.
         *
         * @param json The JSON string to parse
         * @param method The widget method that caused the error (optional, used for error message)
         * @return The parsed exception
         */
        internal fun parse(json: String?, method: LivechatWidgetMethod? = null): LivechatWidgetException {
            var exception = runCatching {
                serializer.deserialize(json, LivechatWidgetException::class.java)
            }.getOrNull() ?: LivechatWidgetException()

            //JSON like "{}" is parsed without error but exception is empty, this make sure we have a valid message and origin
            if (exception.message.isNullOrBlank()) {
                val content = json?.takeIf { it.isNotBlank() } ?: "Unknown error"
                val message = if (method != null) {
                    "${method.name}() $content"
                } else {
                    content
                }
                exception = exception.copy(message = message)
            }

            if (exception.origin.isNullOrBlank()) {
                exception = exception.copy(origin = ORIGIN_LIVECHAT_WIDGET)
            }

            return exception
        }
    }

}

private fun LivechatWidgetException.copy(
    message: String? = this.message,
    code: Int? = this.code,
    name: String? = this.name,
    origin: String? = this.origin,
    platform: String? = this.platform,
): LivechatWidgetException {
    return LivechatWidgetException(message, code, name, origin, platform)
}
