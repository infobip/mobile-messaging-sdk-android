package org.infobip.mobile.messaging.chat.core.widget

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

internal class LivechatWidgetWebViewClient(
    private val widgetWebViewManager: LivechatWidgetWebViewManager
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        widgetWebViewManager.onPageStarted(url)
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        widgetWebViewManager.onPageFinished(url)
        super.onPageFinished(view, url)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        view?.pageDown(true)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString()
        return if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            runCatching {
                view?.context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            }.onFailure { throwable ->
                MobileMessagingLogger.e("Could not open URL.", throwable)
            }.getOrDefault(false)
        } else {
            false
        }
    }
}