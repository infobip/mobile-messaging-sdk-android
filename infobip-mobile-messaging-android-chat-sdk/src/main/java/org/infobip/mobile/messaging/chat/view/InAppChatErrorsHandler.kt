package org.infobip.mobile.messaging.chat.view

import org.infobip.mobile.messaging.chat.core.InAppChatException

/**
 * Error handler allows you to define custom way to process InAppChat related errors.
 * You can use [DefaultInAppChatErrorsHandler] to override only necessary methods.
 */
interface InAppChatErrorsHandler {

    /**
     * Called when an error occurs in InAppChat.
     *
     * @param exception The exception that occurred.
     * @returns true if the error is handled, false to let the SDK handle it.
     */
    fun handleError(exception: InAppChatException): Boolean
}

/**
 * Default implementation of [InAppChatErrorsHandler] with empty methods.
 * It allows you to override only necessary methods.
 */
open class DefaultInAppChatErrorsHandler : InAppChatErrorsHandler {

    override fun handleError(exception: InAppChatException): Boolean {
        return false
    }
}