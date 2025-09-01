package org.infobip.mobile.messaging.chat.core.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachment
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy
import org.infobip.mobile.messaging.chat.models.HasAttachment
import org.infobip.mobile.messaging.chat.models.MessagePayload
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

/**
 * Implementation of [LivechatWidgetClient].
 * Low level communication with JS Livechat Widget from native code.
 */
internal class LivechatWidgetClientImpl(
    private val webView: LivechatWidgetWebView,
    private val instanceId: InstanceId?,
    private val coroutineScope: CoroutineScope,
) : LivechatWidgetClient {

    companion object {
        private const val TAG = "LcWidgetClient"
        private const val MAX_ALLOWED_SCRIPT_LENGTH: Int = 200
        private const val MAX_ALLOWED_ARGUMENT_LENGTH: Int = 50
        private const val ARGUMENT_VISIBLE_PART_LENGTH: Int = 15

        fun shortenLog(log: String): String {
            return InAppChatAttachment.ATTACHMENT_URL_REGEX.replace(log) { matchResult ->
                val prefix = matchResult.groups["prefix"]?.value
                val mimeType = matchResult.groups["mimeType"]?.value
                val base64Prefix = matchResult.groups["base64Prefix"]?.value
                val base64Value = matchResult.groups["base64Value"]?.value
                if ((base64Value?.length ?: 0) > MAX_ALLOWED_ARGUMENT_LENGTH) {
                    val startPart = base64Value?.take(ARGUMENT_VISIBLE_PART_LENGTH)
                    val endPart = base64Value?.takeLast(ARGUMENT_VISIBLE_PART_LENGTH)
                    "$prefix$mimeType$base64Prefix$startPart...$endPart"
                } else {
                    matchResult.toString()
                }
            }
        }
    }

    override fun send(payload: MessagePayload, threadId: String?, executionListener: LivechatWidgetApi.ExecutionListener<String>?) {
        if (payload is HasAttachment && payload.attachment?.isValid == false) {
            executionListener?.onResult(LivechatWidgetResult.Error("Message attachment is not valid."))
            return
        }

        runCatching {
            MessagePayload.serialize(payload)
        }.onFailure {
            executionListener?.onResult(LivechatWidgetResult.Error(it.message ?: "Failed to serialize message payload."))
        }.onSuccess { serializedPayload ->
            if (threadId.isNullOrBlank())
                executeScript(LivechatWidgetMethod.sendMessage.name + "(" + serializedPayload + ")", executionListener)
            else
                executeScript(LivechatWidgetMethod.sendMessage.name + "(" + serializedPayload +", '" + threadId + "')", executionListener)
        }
    }

    override fun createThread(payload: MessagePayload, executionListener: LivechatWidgetApi.ExecutionListener<String>?) {
        if (payload is MessagePayload.Draft) {
            executionListener?.onResult(LivechatWidgetResult.Error("Initial message must not be of type MessagePayload.Draft"))
            return
        }

        if (payload is HasAttachment && payload.attachment?.isValid == false) {
            executionListener?.onResult(LivechatWidgetResult.Error("Message attachment is not valid."))
            return
        }

        runCatching {
            MessagePayload.serialize(payload)
        }.onFailure {
            executionListener?.onResult(LivechatWidgetResult.Error(it.message ?: "Failed to serialize message payload."))
        }.onSuccess { serializedPayload ->
            executeScript(LivechatWidgetMethod.createThread.name + "(" + serializedPayload + ")", executionListener)
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

    override fun getThreads(executionListener: LivechatWidgetApi.ExecutionListener<String>?) {
        executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.getThreads.name), executionListener)
    }

    override fun getActiveThread(executionListener: LivechatWidgetApi.ExecutionListener<String>?) {
        executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.getActiveThread.name), executionListener)
    }

    override fun showThread(threadId: String, executionListener: LivechatWidgetApi.ExecutionListener<String>?) {
        if (threadId.isNotBlank()) {
            executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.showThread.name, threadId), executionListener)
        } else {
            executionListener?.onResult(LivechatWidgetResult.Error("Could not show thread. ThreadId is empty or blank."))
        }
    }

    override fun openNewThread(executionListener: LivechatWidgetApi.ExecutionListener<String>?) {
        executeScript(buildWidgetMethodInvocation(LivechatWidgetMethod.openNewThread.name), executionListener)
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
                val scriptToLog = shortenScript(script)
                MobileMessagingLogger.d(instanceId.tag(TAG), "Called Widget API: $scriptToLog")
                webView.evaluateJavascript(script) { value: String? ->
                    val valueToLog = if ((value != null && "null" != value && "{}" != value)) " => $value" else ""
                    if (valueToLog.isNotEmpty()) {
                        MobileMessagingLogger.d(instanceId.tag(TAG), "Called Widget API: $scriptToLog$valueToLog")
                    }
                    executionListener?.onResult(LivechatWidgetResult.Success(valueToLog))
                }
            }.onFailure {
                executionListener?.onResult(LivechatWidgetResult.Error(it))
                MobileMessagingLogger.e(instanceId.tag(TAG), "Failed to execute webView JS script ${shortenScript(script)} + ${it.message}")
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
                val shortenScript = shortenLog(paramsSubstring)
                builder.append("(")
                builder.append(shortenScript)
                builder.append(")")
            }
            return builder.toString()
        }
        return script
    }
}