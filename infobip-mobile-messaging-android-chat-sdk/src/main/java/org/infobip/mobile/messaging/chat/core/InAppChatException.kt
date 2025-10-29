/*
 * InAppChatException.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core

import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetException
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.json.JSONObject

/**
 * Root exception for all In-App Chat SDK related errors.
 *
 * This open class serves as the base for all chat-related exceptions. Specific exception types
 * are defined as nested classes, allowing for type-safe error handling using `when` expressions
 * and `is` checks.
 *
 * ## Error Codes
 * Error codes are organized into two categories:
 * - **COMMON_CODE_*** (-1xxx): Cross-platform errors that can occur on any platform
 * - **ANDROID_CODE_*** (-2xxx): Android-specific errors
 *
 * @property message Human-readable error message
 * @property code Numeric error code for programmatic identification (see COMMON_CODE_* and ANDROID_CODE_* constants)
 * @property name Machine-readable error name/identifier
 * @property origin The origin/source of the error (SDK, plugin, widget, etc.)
 * @property platform The platform where the error occurred (typically "Android")
 * @property technicalMessage Detailed technical information for debugging purposes
 *
 * @see LivechatWidgetException for Livechat widget-specific errors
 */
open class InAppChatException @JvmOverloads constructor(
    override val message: String? = null,
    val code: Int? = null,
    val name: String? = null,
    val origin: String? = ORIGIN_ANDROID_SDK,
    val platform: String? = PLATFORM_ANDROID,
    val technicalMessage: String? = null,
) : RuntimeException(message) {

    companion object {
        const val PLATFORM_ANDROID = "Android"
        const val ORIGIN_LIVECHAT_WIDGET = "Livechat Widget"
        const val ORIGIN_ANDROID_SDK = "Android SDK"
        const val ORIGIN_REACT_NATIVE_SDK = "React Native SDK"
        const val ORIGIN_FLUTTER_SDK = "Flutter SDK"
        const val ORIGIN_CORDOVA_SDK = "Cordova SDK"

        const val COMMON_CODE_MISSING_PUSH_REGISTRATION_ID = -1001
        const val COMMON_CODE_MISSING_LIVECHAT_WIDGET_ID = -1002
        const val COMMON_CODE_LIVECHAT_WIDGET_API_ERROR = -1003
        const val COMMON_CODE_MOBILE_MESSAGING_ERROR = -1004
        const val COMMON_CODE_NO_INTERNET_CONNECTION = -1005
        const val COMMON_CODE_ATTACHMENT_CREATION_FAILED = -1006
        const val COMMON_CODE_INVALID_MESSAGE_PAYLOAD = -1007

        const val ANDROID_CODE_LIVECHAT_WIDGET_LOADING_TIMEOUT = -2001
        const val ANDROID_CODE_INVALID_LIVECHAT_WIDGET_LOADING_TIMEOUT_VALUE = -2002
        const val ANDROID_CODE_LIVECHAT_WIDGET_WEB_VIEW_NOT_INITIALIZED = -2003
        const val ANDROID_CODE_LIVECHAT_WIDGET_NOT_FOUND = -2004
        const val ANDROID_CODE_JWT_PROVIDER_ERROR = -2005
        const val ANDROID_CODE_CHAT_RESET_EXECUTED = -2006
        const val ANDROID_CODE_CHAT_SERVICE_ERROR = -2007
        const val ANDROID_CODE_MESSAGE_SERIALIZATION_ERROR = -2008
        const val ANDROID_CODE_INVALID_INITIAL_MESSAGE_TYPE = -2009
        const val ANDROID_CODE_INVALID_CONTEXTUAL_DATA = -2010
        const val ANDROID_CODE_INVALID_THREAD_ID = -2011
        const val ANDROID_CODE_INVALID_MESSAGE_ATTACHMENT = -2012
        const val ANDROID_CODE_INVALID_PHOTO_ATTACHMENT_EXTENSION = -2013
        const val ANDROID_CODE_INVALID_VIDEO_ATTACHMENT_EXTENSION = -2014
    }

    fun toJSON(): JSONObject? {
        return try {
            JSONObject().apply {
                putOpt("message", message)
                putOpt("code", code)
                putOpt("name", name)
                putOpt("origin", origin)
                putOpt("platform", platform)
                putOpt("technicalMessage", technicalMessage)
            }
        } catch (e: Exception) {
            MobileMessagingLogger.e("Cannot convert InAppChatException to JSON: ", e)
            null
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine(super.toString())
        code?.let { stringBuilder.appendLine("\tcode=$it,") }
        name?.let { stringBuilder.appendLine("\tname=$it,") }
        origin.let { stringBuilder.appendLine("\torigin=$it,") }
        platform.let { stringBuilder.appendLine("\tplatform=$it") }
        technicalMessage.let { stringBuilder.appendLine("\ttechnicalMessage=$it") }
        return stringBuilder.toString()
    }

    //region Well known exceptions
    class MissingPushRegistrationId : InAppChatException(
        message = "Missing push registration ID.",
        code = COMMON_CODE_MISSING_PUSH_REGISTRATION_ID,
        name = "MISSING_PUSH_REGISTRATION_ID",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = """
           InAppChat mandatory parameter push registration ID is missing. Please follow documentation quick-start guide(https://github.com/infobip/mobile-messaging-sdk-android#quick-start-guide) 
           and make sure MobileMessaging is initialized properly.
        """.trimIndent()
    )

    class MissingLivechatWidgetId : InAppChatException(
        message = "Missing livechat widget ID.",
        code = COMMON_CODE_MISSING_LIVECHAT_WIDGET_ID,
        name = "MISSING_LIVECHAT_WIDGET_ID",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = """
           InAppChat mandatory parameter livechat widget ID is missing. Please follow documentation quick-start guide(https://github
           .com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#quick-start-guide) for correct setup - Livechat Widget connection to Mobile Messaging App profile. Make sure InAppChat 
           is activated using `InAppChat.getInstance(context).activate()`. You can observer `InAppChatEvent.CHAT_CONFIGURATION_SYNCED` broadcast event to be notified when Livechat Widget 
           configuration is synced.
        """.trimIndent()
    )

    class MobileMessagingError(
        mobileMessagingError: org.infobip.mobile.messaging.mobileapi.MobileMessagingError,
    ) : InAppChatException(
        message = "Mobile Messaging SDK error.",
        code = COMMON_CODE_MOBILE_MESSAGING_ERROR,
        name = "MOBILE_MESSAGING_ERROR",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "An error occurred in Mobile Messaging SDK. Caused by MobileMessagingError: ${mobileMessagingError.code} ${mobileMessagingError.message}"
    )

    class NoInternetConnection : InAppChatException(
        message = "No internet connection.",
        code = COMMON_CODE_NO_INTERNET_CONNECTION,
        name = "NO_INTERNET_CONNECTION",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "The device is not connected to the internet. Please check your network settings."
    )

    class AttachmentCreationFailed @JvmOverloads constructor(
        reason: String? = null,
    ) : InAppChatException(
        message = "Failed to create attachment from provided data.",
        code = COMMON_CODE_ATTACHMENT_CREATION_FAILED,
        name = "ATTACHMENT_CREATION_FAILED",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "Failed to create attachment.".withReason(reason)
    )

    class ChatServiceError : InAppChatException(
        message = "Chat service is not available.",
        code = ANDROID_CODE_CHAT_SERVICE_ERROR,
        name = "CHAT_SERVICE_ERROR",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "The chat service is currently unavailable. Please contact Infobip support."
    )

    class LivechatWidgetNotFound : InAppChatException(
        message = "Livechat Widget not found.",
        code = ANDROID_CODE_LIVECHAT_WIDGET_NOT_FOUND,
        name = "LIVECHAT_WIDGET_NOT_FOUND",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = """
           The specified Livechat Widget ID does not exist or is not accessible. Please follow documentation quick-start guide(https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#quick-start-guide) for correct setup - Livechat Widget connection to Mobile Messaging App profile.
        """.trimIndent()
    )

    class InvalidPhotoAttachmentExtension : InAppChatException(
        message = "Photo attachment has invalid file extension.",
        code = ANDROID_CODE_INVALID_PHOTO_ATTACHMENT_EXTENSION,
        name = "INVALID_PHOTO_ATTACHMENT_EXTENSION",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "Photo attachment file extension must be one of the following: jpg, jpeg, png, webp, heic, bmp."
    )

    class InvalidVideoAttachmentExtension : InAppChatException(
        message = "Video attachment has invalid file extension.",
        code = ANDROID_CODE_INVALID_VIDEO_ATTACHMENT_EXTENSION,
        name = "INVALID_VIDEO_ATTACHMENT_EXTENSION",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "Video attachment file extension must be one of the following: mp4, 3gp, webm, m4v, mpeg, mpg, mov, m1v, m2v, mpe, mp4v, mpg4."
    )

    class LivechatWidgetApiError(
        throwable: Throwable?
    ) : InAppChatException(
        message = "Livechat Widget API error.",
        code = COMMON_CODE_LIVECHAT_WIDGET_API_ERROR,
        name = "LIVECHAT_WIDGET_API_ERROR",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "An error occurred while using Livechat Widget API.".withCause(throwable)
    )

    class InvalidMessagePayload @JvmOverloads constructor(
        reason: String? = null,
    ) : InAppChatException(
        message = "Message payload is not valid.",
        code = COMMON_CODE_INVALID_MESSAGE_PAYLOAD,
        name = "INVALID_MESSAGE_PAYLOAD",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "Provided `MessagePayload` is not valid.".withReason(reason)
    )

    class LivechatWidgetLoadingTimeout : InAppChatException(
        message = "Livechat Widget loading timeout.",
        code = ANDROID_CODE_LIVECHAT_WIDGET_LOADING_TIMEOUT,
        name = "LIVECHAT_WIDGET_LOADING_TIMEOUT",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = """
               Livechat Widget loading timeout. You can modify the timeout value if you are experiencing loading issues on slow networks using `loadingTimeoutMillis` property.
            """.trimIndent()
    )

    class LivechatWidgetInvalidLoadingTimeoutValue : InAppChatException(
        message = "Invalid livechat widget loading timeout value.",
        code = ANDROID_CODE_INVALID_LIVECHAT_WIDGET_LOADING_TIMEOUT_VALUE,
        name = "INVALID_LIVECHAT_WIDGET_LOADING_TIMEOUT_VALUE",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "Livechat Widget loading timeout - `loadingTimeoutMillis` value must be greater than 5_000 and less than 300_000 milliseconds."
    )

    class LivechatWidgetWebViewNotInitialized : InAppChatException(
        message = "Livechat Widget WebView not initialized.",
        code = ANDROID_CODE_LIVECHAT_WIDGET_WEB_VIEW_NOT_INITIALIZED,
        name = "LIVECHAT_WIDGET_WEB_VIEW_NOT_INITIALIZED",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = """
                Livechat Widget WebView not initialized. Please raise an issue in GitHub(https://github.com/infobip/mobile-messaging-sdk-android/issues) and provide details how you use InAppChat.
            """.trimIndent()
    )

    class JwtProviderError(
        throwable: Throwable?
    ) : InAppChatException(
        message = "`JwtProvider` returned error.",
        code = ANDROID_CODE_JWT_PROVIDER_ERROR,
        name = "JWT_PROVIDER_ERROR",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "An error occurred while obtaining JWT token from the `JwtProvider` you set in InAppChat.".withCause(throwable)
    )

    class ChatResetExecuted : InAppChatException(
        message = "Chat has been reset and is no longer loaded.",
        code = ANDROID_CODE_CHAT_RESET_EXECUTED,
        name = "CHAT_RESET_EXECUTED",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "The chat has been reset, previous Livechat Widget session is no longer valid. Load chat again to start a new session."
    )

    class InvalidInitialMessageType : InAppChatException(
        message = "Initial message must not be of type Draft.",
        code = ANDROID_CODE_INVALID_INITIAL_MESSAGE_TYPE,
        name = "INVALID_INITIAL_MESSAGE_TYPE",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "When creating a new thread, the initial `MessagePayload` must not be of type `MessagePayload.Draft`."
    )

    class InvalidContextualData : InAppChatException(
        message = "Contextual data are not valid.",
        code = ANDROID_CODE_INVALID_CONTEXTUAL_DATA,
        name = "INVALID_CONTEXTUAL_DATA",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "Provided contextual data must not be null or blank."
    )

    class InvalidThreadId : InAppChatException(
        message = "Thread ID is not valid.",
        code = ANDROID_CODE_INVALID_THREAD_ID,
        name = "INVALID_THREAD_ID",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "Provided thread ID must not be null or blank."
    )

    class MessageSerializationError @JvmOverloads constructor(
        throwable: Throwable? = null,
    ) : InAppChatException(
        message = "Failed to serialize message payload.",
        code = ANDROID_CODE_MESSAGE_SERIALIZATION_ERROR,
        name = "MESSAGE_SERIALIZATION_ERROR",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "An error occurred while serializing `MessagePayload` to be sent to Livechat Widget.".withCause(throwable)
    )

    class InvalidMessageAttachment : InAppChatException(
        message = "Message attachment is not valid.",
        code = ANDROID_CODE_INVALID_MESSAGE_ATTACHMENT,
        name = "INVALID_MESSAGE_ATTACHMENT",
        origin = ORIGIN_ANDROID_SDK,
        platform = PLATFORM_ANDROID,
        technicalMessage = "Provided `InAppChatAttachment` must contains valid - non blank mimeType, base64 and fileName properties."
    )
    //endregion

}

private fun String.withCause(throwable: Throwable?): String {
    if (throwable == null) return this
    return "$this Caused by ${throwable.javaClass.simpleName}${throwable.message?.let { ": $it" } ?: ""}"
}

private fun String.withReason(reason: String?): String {
    if (reason.isNullOrBlank()) return this
    return "$this Reason: $reason"
}
