package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.chat.core.InAppChatWidgetView

enum class LivechatWidgetView {
    LOADING,
    THREAD_LIST,
    LOADING_THREAD,
    THREAD,
    CLOSED_THREAD,
    SINGLE_MODE_THREAD,
}

internal fun LivechatWidgetView.toInAppChatWidgetView(): InAppChatWidgetView {
    return when (this) {
        LivechatWidgetView.LOADING -> InAppChatWidgetView.LOADING
        LivechatWidgetView.THREAD_LIST -> InAppChatWidgetView.THREAD_LIST
        LivechatWidgetView.LOADING_THREAD -> InAppChatWidgetView.LOADING_THREAD
        LivechatWidgetView.THREAD -> InAppChatWidgetView.THREAD
        LivechatWidgetView.CLOSED_THREAD -> InAppChatWidgetView.CLOSED_THREAD
        LivechatWidgetView.SINGLE_MODE_THREAD -> InAppChatWidgetView.SINGLE_MODE_THREAD
    }
}