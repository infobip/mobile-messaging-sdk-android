package org.infobip.mobile.messaging.chat.core.widget

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

@SuppressLint("SetJavaScriptEnabled")
internal class LivechatWidgetWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WebView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        const val TAG = "LivechatWidgetWebView"
        private const val DEFAULT_WIDGET_URI = "https://livechat.infobip.com/widget.js"
    }

    private val widgetPageUri = context.getString(R.string.ib_livechat_widget_page_uri)
    private val widgetUri = context.getString(R.string.ib_livechat_widget_uri)

    var livechatWidgetClient: LivechatWidgetClient? = null

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = LOAD_NO_CACHE
        }
        isClickable = true
        webChromeClient = LivechatWidgetWebChromeClient()
    }

    fun setup(
        webViewManager: LivechatWidgetWebViewManager,
        coroutineScope: CoroutineScope
    ) {
        webViewClient = LivechatWidgetWebViewClient(webViewManager)
        addJavascriptInterface(LivechatWidgetJsInterfaceImpl(webViewManager, coroutineScope), LivechatWidgetJsInterface.name)
        livechatWidgetClient = LivechatWidgetClientImpl(this, coroutineScope)
    }

    fun loadWidgetPage(
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