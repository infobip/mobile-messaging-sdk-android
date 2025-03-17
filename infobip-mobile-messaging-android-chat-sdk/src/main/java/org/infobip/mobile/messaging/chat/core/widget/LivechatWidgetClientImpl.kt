package org.infobip.mobile.messaging.chat.core.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

/**
 * Implementation of [LivechatWidgetClient].
 * Low level communication with JS Livechat Widget from native code.
 */
internal class LivechatWidgetClientImpl(
    private val webView: LivechatWidgetWebView,
    private val coroutineScope: CoroutineScope,
) : LivechatWidgetClient {

    companion object {
        private const val TAG = "LivechatWidgetClient"
        private const val MAX_ALLOWED_SCRIPT_LENGTH: Int = 200
        private const val MAX_ALLOWED_ARGUMENT_LENGTH: Int = 50
        private const val ARGUMENT_VISIBLE_PART_LENGTH: Int = 15
    }

    override fun sendMessage(
        message: String?,
        attachment: InAppChatMobileAttachment?,
        executionListener: LivechatWidgetApi.ExecutionListener<String>?
    ) {
        val attachmentBase64 = attachment?.base64UrlString()
        val fileName = attachment?.fileName

        val script = when {
            attachmentBase64?.isNotBlank() == true -> buildWidgetMethodInvocation(LivechatWidgetMethod.sendMessageWithAttachment.name, message, attachmentBase64, fileName)
            message?.isNotBlank() == true -> buildWidgetMethodInvocation(LivechatWidgetMethod.sendMessage.name, message)
            else -> null
        }

        if (script != null) {
            executeScript(script, executionListener)
        } else {
            executionListener?.onResult(LivechatWidgetResult.Error("Could not send message. Both message and attachment are null or empty."))
        }
    }

    override fun sendDraft(
        draft: String?,
        executionListener: LivechatWidgetApi.ExecutionListener<String>?
    ) {
        if (draft?.isNotBlank() == true) {
            executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.sendDraft.name, draft), executionListener)
        } else {
            executionListener?.onResult(LivechatWidgetResult.Error("Could not send draft. Draft is null or empty."))
        }
    }

    override fun setLanguage(
        language: LivechatWidgetLanguage,
        executionListener: LivechatWidgetApi.ExecutionListener<String>?
    ) {
        executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.setLanguage.name, language.widgetCode), executionListener)
    }

    override fun sendContextualData(
        data: String,
        multiThreadFlag: MultithreadStrategy,
        executionListener: LivechatWidgetApi.ExecutionListener<String>?
    ) {
        if (data.isNotBlank()) {
            executeScript(LivechatWidgetMethod.sendContextualData.name + "(" + data + ", '" + multiThreadFlag + "')", executionListener)
        } else {
            executionListener?.onResult(LivechatWidgetResult.Error("Could not send contextual data. Data is null or empty."))
        }
    }

    override fun showThreadList(
        executionListener: LivechatWidgetApi.ExecutionListener<String>?
    ) {
        executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.showThreadList.name), executionListener)
    }

    override fun pauseConnection(
        executionListener: LivechatWidgetApi.ExecutionListener<String>?
    ) {
        executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.pauseConnection.name), executionListener)
    }

    override fun resumeConnection(
        executionListener: LivechatWidgetApi.ExecutionListener<String>?
    ) {
        executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.resumeConnection.name), executionListener)
    }

    override fun setTheme(
        themeName: String?,
        executionListener: LivechatWidgetApi.ExecutionListener<String>?
    ) {
        executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.setTheme.name, themeName), executionListener)
    }

    /**
     * Executes JS script on UI thread with result listener.
     *
     * @param script         to be executed
     * @param executionListener notify about result
     */
    private fun executeScript(script: String, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                webView.evaluateJavascript(script) { value: String? ->
                    val valueToLog = if ((value != null && "null" != value)) " => $value" else ""
                    val scriptToLog = shortenScript(script)
                    MobileMessagingLogger.d(TAG, "Called Widget API: $scriptToLog$valueToLog")
                    executionListener?.onResult(LivechatWidgetResult.Success(valueToLog))
                }
            }.onFailure {
                executionListener?.onResult(LivechatWidgetResult.Error(it))
                MobileMessagingLogger.e(TAG, "Failed to execute webView JS script ${shortenScript(script)} + ${it.message}")
            }
        }
    }

    private fun buildWidgetMethodInvocation(methodName: String, vararg params: String?): String {
        val builder = StringBuilder()
        builder.append(methodName)

        if (params.isNotEmpty()) {
            builder.append(params.joinToString("','", "('", "')"))
        } else {
            builder.append("()")
        }

        return builder.toString()
    }

    private fun shortenScript(script: String?): String? {
        if (script != null && script.length > MAX_ALLOWED_SCRIPT_LENGTH) {
            val builder = StringBuilder()
            val methodNameEndIndex = script.indexOf("(")
            if (methodNameEndIndex > 0) {
                val methodName = script.substring(0, methodNameEndIndex)
                builder.append(methodName)
                val paramsSubstring = script.substring(methodNameEndIndex + 1, script.length - 1)
                val params: List<String> = paramsSubstring.split(",".toRegex())
                if (params.isNotEmpty()) {
                    val shortenedParams = mutableListOf<String>()
                    for (param in params) {
                        var value = param.replace("'", "")
                        if (value.length > MAX_ALLOWED_ARGUMENT_LENGTH) {
                            value = value.substring(0, ARGUMENT_VISIBLE_PART_LENGTH) + "..." + value.substring(value.length - ARGUMENT_VISIBLE_PART_LENGTH)
                        }
                        shortenedParams.add(value)
                    }
                    builder.append(shortenedParams.joinToString("','", "('", "')"))
                } else {
                    builder.append("()")
                }
            }
            return builder.toString()
        }
        return script
    }
}