package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

/**
 * Represents Livechat widget thread status.
 */
enum class LivechatWidgetThreadStatus {
    OPEN,
    SOLVED,
    CLOSED,
}

/**
 * Represents Livechat widget thread.
 */
data class LivechatWidgetThread(
    val id: String? = null,
    val conversationId: String? = null,
    val status: LivechatWidgetThreadStatus? = null,
    val raw: String? = null,
) {

    internal companion object {
        private val serializer = JsonSerializer(false)

        internal fun parseOrNull(json: String?): LivechatWidgetThread? {
            return json?.takeIf { it.isNotBlank() && it != "null" }?.let {
                return runCatching {
                    serializer.deserialize(json, LivechatWidgetThread::class.java).copy(raw = json)
                }.onFailure {
                    MobileMessagingLogger.e("Could not parse thread from: $json", it)
                }.getOrNull()
            }
        }

        internal fun parse(json: String): LivechatWidgetThread {
            return parseOrNull(json) ?: LivechatWidgetThread(raw = json)
        }
    }
}

/**
 * Represents Livechat widget threads.
 */
data class LivechatWidgetThreads(
    val threads: List<LivechatWidgetThread>? = null,
    val raw: String? = null,
) {

    internal companion object {
        private val serializer = JsonSerializer(false)

        internal fun parse(json: String): LivechatWidgetThreads {
            return runCatching {
                val threads = serializer.deserialize(json, Array<LivechatWidgetThread>::class.java)
                LivechatWidgetThreads(threads = threads.toList(), raw = json)
            }.onFailure {
                MobileMessagingLogger.e("Could not parse threads from: $json", it)
            }.getOrDefault(LivechatWidgetThreads(raw = json))
        }
    }

}