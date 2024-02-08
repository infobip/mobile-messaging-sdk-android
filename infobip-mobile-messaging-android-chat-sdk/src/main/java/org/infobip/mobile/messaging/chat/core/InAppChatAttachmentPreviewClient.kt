package org.infobip.mobile.messaging.chat.core

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import org.infobip.mobile.messaging.MobileMessaging.ResultListener
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError
import org.infobip.mobile.messaging.mobileapi.Result

/**
 * InAppChat preview WebView client.
 */
internal interface InAppChatAttachmentPreviewClient {
    fun downloadAttachment()
}

internal class InAppChatAttachmentPreviewClientImpl(
    private val webView: WebView
): InAppChatAttachmentPreviewClient {

    private val TAG = "InAppChatAttachmentPreviewClient"
    private val handler = Handler(Looper.getMainLooper())

    override fun downloadAttachment() {
        executeScript(buildWidgetMethodInvocation(InAppChatAttachmentPreviewMethods.downloadAttachment))
    }

    private fun executeScript(script: String, resultListener: ResultListener<String>? = null) {
        try {
            handler.post {
                webView.evaluateJavascript(script) { value: String? ->
                    val valueToLog = if (value != null && "null" != value) ":$value" else ""
                    MobileMessagingLogger.d(TAG, "$script$valueToLog")
                    resultListener?.onResult(Result(valueToLog))
                }
            }
        } catch (e: Exception) {
            resultListener?.onResult(Result(MobileMessagingError.createFrom(e)))
            MobileMessagingLogger.e("Failed to execute webView JS script" + e.message)
        }
    }

    private fun buildWidgetMethodInvocation(
        method: InAppChatAttachmentPreviewMethods,
        vararg params: String
    ): String {
        val builder = StringBuilder()
        builder.append(method.name)
        if (params.isNotEmpty()) {
            val resultParamsStr = params.joinToString(separator = "','", prefix = "('", postfix = "')")
            builder.append(resultParamsStr)
        } else {
            builder.append("()")
        }
        return builder.toString()
    }

}

