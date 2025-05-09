package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.chat.attachments.AttachmentHelper
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.json.JSONObject

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
    ) : LivechatWidgetMessage(){
        val threadId get() = thread?.id
    }

    data class Basic(
        val message: String? = null,
        val attachment: InAppChatMobileAttachment? = null,
        val thread: LivechatWidgetThread? = null,
    ) : LivechatWidgetMessage() {
        val threadId get() = thread?.id
    }

    data class CustomData(
        val customData: String? = null,
        val agentMessage: String? = null,
        val userMessage: String? = null,
        val thread: LivechatWidgetThread? = null,
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
    }

    override fun getCls(): Class<LivechatWidgetMessage> = LivechatWidgetMessage::class.java

    override fun deserialize(value: String?): LivechatWidgetMessage? {
        return value?.let {
            runCatching {
                val jsonObject = JSONObject(it)
                if (jsonObject.has(MESSAGE_TYPE)) {
                    when (LivechatWidgetMessageType.valueOf(jsonObject.getString(MESSAGE_TYPE))) {
                        LivechatWidgetMessageType.DRAFT -> LivechatWidgetMessage.Draft(
                            message = jsonObject.optString(LivechatWidgetMessage.Draft::message.name).takeIf { it.isNotBlank() },
                            thread = jsonObject.getJSONObject(LivechatWidgetMessage.Draft::thread.name).toString().takeIf { it.isNotBlank() }?.let {
                                LivechatWidgetThread.parseOrNull(it)
                            }
                        )

                        LivechatWidgetMessageType.BASIC -> {
                            LivechatWidgetMessage.Basic(
                                message = jsonObject.optString(LivechatWidgetMessage.Basic::message.name).takeIf { it.isNotBlank() },
                                attachment = deserializeAttachment(
                                    jsonObject.optString(LivechatWidgetMessage.Basic::attachment.name),
                                    jsonObject.optString(ATTACHMENT_FILE_NAME),
                                ),
                                thread = jsonObject.getJSONObject(LivechatWidgetMessage.Basic::thread.name).toString().takeIf { it.isNotBlank() }?.let {
                                    LivechatWidgetThread.parseOrNull(it)
                                }
                            )
                        }

                        LivechatWidgetMessageType.CUSTOM_DATA -> LivechatWidgetMessage.CustomData(
                            customData = jsonObject.getJSONObject(LivechatWidgetMessage.CustomData::customData.name).toString().takeIf { it.isNotBlank() },
                            agentMessage = jsonObject.optString(LivechatWidgetMessage.CustomData::agentMessage.name).takeIf { it.isNotBlank() },
                            userMessage = jsonObject.optString(LivechatWidgetMessage.CustomData::userMessage.name).takeIf { it.isNotBlank() },
                            thread = jsonObject.getJSONObject(LivechatWidgetMessage.CustomData::thread.name).toString().takeIf { it.isNotBlank() }?.let {
                                LivechatWidgetThread.parseOrNull(it)
                            }
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

    private fun deserializeAttachment(
        attachment: String,
        fileName: String,
    ): InAppChatMobileAttachment? {
        return runCatching {
            if (attachment.isNotBlank()) {
                AttachmentHelper.ATTACHMENT_URL_REGEX.find(attachment)?.let { matchResult ->
                    val mimeType = matchResult.groups["mimeType"]?.value
                    val base64Value = matchResult.groups["base64Value"]?.value
                    InAppChatMobileAttachment(mimeType, base64Value, fileName).takeIf { it.isValid }
                }
            } else {
                null
            }
        }.getOrNull()
    }

    override fun serialize(value: LivechatWidgetMessage?): String? {
        return null
    }

}