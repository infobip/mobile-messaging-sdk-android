package org.infobip.mobile.messaging.chat.view

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.core.InAppChatMobileImpl
import org.infobip.mobile.messaging.chat.core.InAppChatWebChromeClient
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewClient
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewManager
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

@SuppressLint("SetJavaScriptEnabled")
internal class InAppChatWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WebView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val TAG = "InAppChatWebView"
        private const val IN_APP_CHAT_MOBILE_INTERFACE = "InAppChatMobile"
        private const val DEFAULT_WIDGET_URI = "https://livechat.infobip.com/widget.js"
    }

    private val widgetPageUri = context.getString(R.string.ib_inappchat_widget_page_uri)
    private val widgetUri = context.getString(R.string.ib_inappchat_widget_uri)

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = LOAD_NO_CACHE
        }
        isClickable = true
        webChromeClient = InAppChatWebChromeClient()
    }

    fun setup(webViewManager: InAppChatWebViewManager) {
        webViewClient = InAppChatWebViewClient(webViewManager)
        addJavascriptInterface(InAppChatMobileImpl(webViewManager), IN_APP_CHAT_MOBILE_INTERFACE)
    }

    fun loadChatPage(
        pushRegistrationId: String,
        widgetId: String,
        jwt: String? = null,
        domain: String? = null,
        widgetTheme: String? = null,
    ) {
        val builder = Uri.Builder()
            .encodedPath(widgetPageUri)
            .appendQueryParameter("pushRegId", pushRegistrationId)
            .appendQueryParameter("widgetId", widgetId)

        val encodedWidgetUri = widgetUri.takeIf { it != DEFAULT_WIDGET_URI }?.encode()

        if (encodedWidgetUri?.isNotBlank() == true) {
            builder.appendQueryParameter("widgetUri", encodedWidgetUri)
        }

        if (jwt?.isNotBlank() == true) {
            builder.appendQueryParameter("jwt", jwt)
        }

        if (domain?.isNotBlank() == true) {
            builder.appendQueryParameter("domain", domain)
        }

        if (widgetTheme?.isNotBlank() == true) {
            builder.appendQueryParameter("widgetTheme", widgetTheme)
        }

        val resultUrl = builder.build().toString()
        MobileMessagingLogger.d(TAG, "Loading page: $resultUrl")
        loadUrl(resultUrl)
    }

    private fun String.encode(): String = Uri.encode(this)

}