package org.infobip.mobile.messaging.chat.core.widget

import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import org.infobip.mobile.messaging.chat.BuildConfig
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

internal class LivechatWidgetWebChromeClient(
    private val instanceId: InstanceId?,
) : WebChromeClient() {

    init {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        if (BuildConfig.DEBUG)
            MobileMessagingLogger.d(instanceId.tag(LivechatWidgetWebView.TAG), consoleMessage.format())
        return true
    }

    override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        view?.requestFocus()
    }

    private fun ConsoleMessage.format(): String = "${lineNumber()}   ${sourceId()}   ${message()}"

}