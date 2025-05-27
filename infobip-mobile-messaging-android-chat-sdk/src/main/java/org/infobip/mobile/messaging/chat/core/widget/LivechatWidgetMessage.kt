package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.chat.attachments.AttachmentHelper
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.json.JSONObject
import java.util.Date

/**
 * Represents Livechat widget message response.
 */
sealed class LivechatWidgetMessage {

    internal companion object {
        private val serializer = JsonSerializer(false, LivechatWidgetMessageAdapter())

        internal fun parseOrNull(json: String?): LivechatWidgetMessage? {
            return json?.takeIf { it.isNotBlank() && it != "null" }?.let {
                runCatching {
                    serializer.deserialize(it, LivechatWidgetMessage::class.java)
                }.onFailure {
                    MobileMessagingLogger.e("Could not parse message from: $json", it)
                }.getOrNull()
            }
        }

        internal fun parse(json: String?): LivechatWidgetMessage {
            return parseOrNull(json) ?: Unknown(json)
        }
    }

    data class Draft(
        val message: String? = null,
        val thread: LivechatWidgetThread? = null,
    ) : LivechatWidgetMessage() {
        val threadId get() = thread?.id
    }

    data class Basic(
        val message: String? = null,
        val attachment: InAppChatMobileAttachment? = null,
        val thread: LivechatWidgetThread? = null,
        val moment: Date? = null,
        val id: String? = null,
    ) : LivechatWidgetMessage() {
        val threadId get() = thread?.id
    }

    data class CustomData(
        val customData: String? = null,
        val agentMessage: String? = null,
        val userMessage: String? = null,
        val thread: LivechatWidgetThread? = null,
        val moment: Date? = null,
        val id: String? = null,
    ) : LivechatWidgetMessage() {
        val threadId get() = thread?.id
    }

    data class Unknown(
        val raw: String? = null,
    ) : LivechatWidgetMessage()

}

internal class LivechatWidgetMessageAdapter : JsonSerializer.ObjectAdapter<LivechatWidgetMessage> {

    companion object {
        private const val TAG = "LivechatWidgetMessageAdapter"
        private const val MESSAGE_TYPE = "type"
        private const val ATTACHMENT_FILE_NAME = "fileName"
        private const val THREAD = "thread"
        private const val THREAD_ID = "threadId"
        private const val MESSAGE = "message"
        private const val CONTENT = "content"
        private const val TEXT = "text"
    }

    override fun getCls(): Class<LivechatWidgetMessage> = LivechatWidgetMessage::class.java

    override fun deserialize(value: String?): LivechatWidgetMessage? {
        return value?.let {
            runCatching {
                val root = JSONObject(it)
                val messageJsonObject = root.getJSONObject(MESSAGE)
                val contentJsonObject = messageJsonObject.optJSONObject(CONTENT)
                if (messageJsonObject.has(MESSAGE_TYPE)) {
                    when (LivechatWidgetMessageType.valueOf(messageJsonObject.getString(MESSAGE_TYPE))) {
                        LivechatWidgetMessageType.DRAFT -> LivechatWidgetMessage.Draft(
                            message = contentJsonObject?.optString(TEXT).takeIf { it?.isNotBlank() == true },
                            thread = deserializeThread(root)
                        )

                        LivechatWidgetMessageType.BASIC -> LivechatWidgetMessage.Basic(
                            message = contentJsonObject?.optString(TEXT).takeIf { it?.isNotBlank() == true },
                            attachment = contentJsonObject?.let { deserializeAttachment(it) },
                            thread = deserializeThread(root),
                            moment = deserializeMoment(messageJsonObject, LivechatWidgetMessage.Basic::moment.name),
                            id = messageJsonObject.optString(LivechatWidgetMessage.Basic::id.name).takeIf { it.isNotBlank() }
                        )

                        LivechatWidgetMessageType.CUSTOM_DATA -> LivechatWidgetMessage.CustomData(
                            customData = contentJsonObject?.optJSONObject(LivechatWidgetMessage.CustomData::customData.name)?.toString()?.takeIf { it.isNotBlank() },
                            agentMessage = contentJsonObject?.optString(LivechatWidgetMessage.CustomData::agentMessage.name)?.takeIf { it.isNotBlank() },
                            userMessage = contentJsonObject?.optString(LivechatWidgetMessage.CustomData::userMessage.name)?.takeIf { it.isNotBlank() },
                            thread = deserializeThread(root),
                            moment = deserializeMoment(messageJsonObject, LivechatWidgetMessage.CustomData::moment.name),
                            id = messageJsonObject.optString(LivechatWidgetMessage.CustomData::id.name).takeIf { it.isNotBlank() }
                        )
                    }
                } else {
                    LivechatWidgetMessage.Unknown(it)
                }
            }.getOrElse {
                MobileMessagingLogger.e(TAG, "Could not parse message from: $value", it)
                LivechatWidgetMessage.Unknown(value)
            }
        }
    }

    private fun deserializeMoment(jsonObject: JSONObject, key: String): Date? {
        return jsonObject.optLong(key).takeIf { it != 0L }?.let {
            runCatching { Date(it) }.getOrNull()
        }
    }

    private fun deserializeThread(jsonObject: JSONObject): LivechatWidgetThread? {
        return runCatching {
            var thread: LivechatWidgetThread? = null
            if (jsonObject.has(THREAD)) {
                thread = jsonObject.optJSONObject(THREAD)?.toString()?.takeIf { it.isNotBlank() }?.let {
                    LivechatWidgetThread.parseOrNull(it)
                }
            }
            if (jsonObject.has(THREAD_ID) && thread?.id == null) {
                thread = LivechatWidgetThread(id = jsonObject.optString(THREAD_ID))
            }
            return thread
        }.getOrNull()
    }

    private fun deserializeAttachment(jsonObject: JSONObject): InAppChatMobileAttachment? {
        return runCatching {
            var attachment: InAppChatMobileAttachment? = null
            if (jsonObject.has(LivechatWidgetMessage.Basic::attachment.name)) {
                val attachmentUrl = jsonObject.optString(LivechatWidgetMessage.Basic::attachment.name).takeIf { it.isNotBlank() }
                val fileName = jsonObject.optString(ATTACHMENT_FILE_NAME).takeIf { it.isNotBlank() }
                if (attachmentUrl?.isNotBlank() == true) {
                    attachment = AttachmentHelper.ATTACHMENT_URL_REGEX.find(attachmentUrl)?.let { matchResult ->
                        val mimeType = matchResult.groups["mimeType"]?.value
                        val base64Value = matchResult.groups["base64Value"]?.value
                        InAppChatMobileAttachment(mimeType, base64Value, fileName).takeIf { it.isValid }
                    }
                }
            }
            return attachment
        }.getOrNull()
    }

    override fun serialize(value: LivechatWidgetMessage?): String? {
        return null
    }

}