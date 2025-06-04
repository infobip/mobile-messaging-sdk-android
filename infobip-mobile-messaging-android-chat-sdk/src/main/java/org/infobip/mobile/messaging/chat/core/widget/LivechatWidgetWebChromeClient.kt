package org.infobip.mobile.messaging.chat.core.widget

import android.webkit.ConsoleMessage
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

    private fun ConsoleMessage.format(): String = "${lineNumber()}   ${sourceId()}   ${message()}"

}