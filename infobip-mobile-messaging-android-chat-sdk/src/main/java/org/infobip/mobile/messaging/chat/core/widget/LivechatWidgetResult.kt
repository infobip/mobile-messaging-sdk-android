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
        constructor(message: String) : this(Exception(message))
    }

    fun isSuccess() = this is Success
    fun isError() = this is Error
    fun getOrNull(): T? = if (this is Success) payload else null
    fun errorOrNull(): Throwable? = if (this is Error) throwable else null

    internal fun addErrorMessagePrefix(prefix: String): LivechatWidgetResult<T> {
        return when (this) {
            is Error -> this.copy(throwable = Exception("$prefix ${throwable.message.orEmpty()}"))
            is Success -> this
        }
    }
}