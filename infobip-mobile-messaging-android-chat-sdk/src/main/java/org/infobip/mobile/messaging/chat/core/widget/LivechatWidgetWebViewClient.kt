/*
 * LivechatWidgetWebViewClient.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core.widget

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

internal class LivechatWidgetWebViewClient(
    private val widgetWebViewManager: LivechatWidgetWebViewManager,
    private val instanceId: InstanceId?,
) : WebViewClient() {

    companion object {
        private const val TAG = "LcWidgetWebViewClient"
    }

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

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        super.onReceivedSslError(view, handler, error)
        MobileMessagingLogger.e(instanceId.tag(TAG), "onReceivedSslError(): $error")
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        widgetWebViewManager.onWidgetUrlInteracted(view, request)
        return true //mark as handled = never override localhost chat page url
    }
}