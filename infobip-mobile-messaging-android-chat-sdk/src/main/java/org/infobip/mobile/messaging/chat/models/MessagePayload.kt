package org.infobip.mobile.messaging.chat.models

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachment
import org.infobip.mobile.messaging.chat.core.InAppChatException
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetMessageType
import org.json.JSONObject

/**
 * Interface for classes that have an attachment.
 */
interface HasAttachment {
    val attachment: InAppChatAttachment?
}

/**
 * Base class for all message payload types of livechat widget.
 *
 * Each subclass represents a specific kind of message that can be sent, such as text, draft, attachment, or custom data.
 *
 * @property type The type identifier of the message, used to distinguish payload variants.
 */
sealed class MessagePayload(
    val type: LivechatWidgetMessageType
) {

    internal companion object {
        private val serializer = JsonSerializer(false, MessagePayloadAdapter())

        internal fun serialize(message: MessagePayload): String? {
            return serializer.serialize(message)
        }
    }

    /**
     * Represents a draft message.
     *
     * @property message The text content of the draft. Must be non-blank and no longer than
     * [LivechatWidgetApi.MESSAGE_MAX_LENGTH] characters.
     *
     * @throws InAppChatException If [message] is blank or exceeds the maximum allowed length.
     */
    data class Draft @Throws(InAppChatException::class) constructor(
        val message: String
    ) : MessagePayload(LivechatWidgetMessageType.DRAFT) {
        init {
            if (message.isBlank())
                throw InAppChatException.InvalidMessagePayload("Draft message cannot be blank.")
            if (message.length > LivechatWidgetApi.MESSAGE_MAX_LENGTH)
                throw InAppChatException.InvalidMessagePayload("Draft message length exceeds the maximum allowed length of ${LivechatWidgetApi.MESSAGE_MAX_LENGTH} characters.")
        }
    }

    /**
     * Represents a basic text message, optionally containing an attachment.
     *
     * Either [message] or [attachment] must be provided.
     *
     * @property message The text content of the message. Must be non-blank and no longer than
     * [LivechatWidgetApi.MESSAGE_MAX_LENGTH] characters, if present.
     * @property attachment An optional attachment associated with the message. Must be valid if provided.
     *
     * @throws InAppChatException If both [message] and [attachment] are null,
     * if [message] is blank or too long, or if [attachment] is invalid.
     */
    data class Basic @Throws(InAppChatException::class) @JvmOverloads constructor(
        val message: String?,
        override val attachment: InAppChatAttachment? = null,
    ) : MessagePayload(LivechatWidgetMessageType.BASIC), HasAttachment {
        init {
            if (message == null && attachment == null)
                throw InAppChatException.InvalidMessagePayload("Either message or attachment must be provided.")
            if (message != null) {
                if (message.isBlank())
                    throw InAppChatException.InvalidMessagePayload("Message cannot be blank.")
                if (message.length > LivechatWidgetApi.MESSAGE_MAX_LENGTH)
                    throw InAppChatException.InvalidMessagePayload("Message length exceeds the maximum allowed length of ${LivechatWidgetApi.MESSAGE_MAX_LENGTH} characters.")
            }
            if (attachment != null && !attachment.isValid)
                throw InAppChatException.InvalidMessageAttachment()
        }
    }

    /**
     * Represents a message containing structured custom data related to the conversation.
     *
     * This type of message can optionally include contextual text from the agent and/or the user.
     *
     * @property customData A JSON string representing the custom data payload. Must be a valid JSON object.
     * @property agentMessage An optional message visible only to the agent providing context for the custom data.
     * @property userMessage An optional message visible only to the user and agent related to the custom data.
     */
    data class CustomData @JvmOverloads constructor(
        val customData: String,
        val agentMessage: String? = null,
        val userMessage: String? = null
    ) : MessagePayload(LivechatWidgetMessageType.CUSTOM_DATA)

}

internal class MessagePayloadAdapter : JsonSerializer.ObjectAdapter<MessagePayload> {

    override fun getCls(): Class<MessagePayload> = MessagePayload::class.java

    override fun deserialize(value: String?): MessagePayload? {
        return null
    }

    override fun serialize(value: MessagePayload?): String? {
        return when (value) {
            is MessagePayload.Draft -> return JSONObject().apply {
                put("message", value.message)
                put("type", value.type.name)
            }.toString()

            is MessagePayload.Basic -> return JSONObject().apply {
                if (value.message?.isNotBlank() == true) {
                    put("message", value.message)
                }
                if (value.attachment != null) {
                    put("attachment", "data:" + value.attachment.mimeType + ";base64," + value.attachment.base64)
                    put("fileName", value.attachment.fileName)
                }
                put("type", value.type.name)
            }.toString()

            is MessagePayload.CustomData -> return JSONObject().apply {
                put("customData", value.customData)
                if (value.agentMessage != null) {
                    put("agentMessage", value.agentMessage)
                }
                if (value.userMessage != null) {
                    put("userMessage", value.userMessage)
                }
                put("type", value.type.name)
            }.toString()

            null -> null
        }
    }

}
