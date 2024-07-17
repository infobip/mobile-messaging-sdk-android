package org.infobip.mobile.messaging.chat.view

/**
 * Error handler allows you to define custom way to process InAppChat related errors.
 */
interface InAppChatErrorHandler {
    fun handlerError(error: String)
    fun handlerWidgetError(error: String)
    fun handlerNoInternetConnectionError(hasConnection: Boolean)
}