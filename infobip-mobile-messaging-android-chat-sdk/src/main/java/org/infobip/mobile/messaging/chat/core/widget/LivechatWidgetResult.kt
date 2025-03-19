package org.infobip.mobile.messaging.chat.core.widget

/**
 * It represents result of Livechat Widget API actions.
 */
sealed class LivechatWidgetResult<out T> {

    data class Success<T>(val payload: T) : LivechatWidgetResult<T>() {
        companion object {
            @JvmStatic
            val unit = Success(Unit)
        }
    }

    data class Error(
        val throwable: Throwable,
    ) : LivechatWidgetResult<Nothing>() {
        constructor(message: String) : this(LivechatWidgetException.fromAndroid(message))
    }

    val isSuccess
        get() = this is Success
    val isError
        get() = this is Error
    fun getOrNull(): T? = if (this is Success) payload else null
    fun errorOrNull(): Throwable? = if (this is Error) throwable else null
}