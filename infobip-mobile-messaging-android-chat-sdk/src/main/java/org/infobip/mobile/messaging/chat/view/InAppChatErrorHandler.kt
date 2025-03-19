package org.infobip.mobile.messaging.chat.view

/**
 * Error handler allows you to define custom way to process InAppChat related errors.
 * You can use [DefaultInAppChatErrorHandler] to override only necessary methods.
 */
interface InAppChatErrorHandler {
    fun handlerError(error: String)
    fun handlerWidgetError(error: String)
    fun handlerNoInternetConnectionError(hasConnection: Boolean)
}

/**
 * Default implementation of [InAppChatErrorHandler] with empty methods.
 * It allows you to override only necessary methods.
 */
open class DefaultInAppChatErrorHandler : InAppChatErrorHandler {
    override fun handlerError(error: String) {}
    override fun handlerWidgetError(error: String) {}
    override fun handlerNoInternetConnectionError(hasConnection: Boolean) {}
}