/*
 * LivechatWidgetWebView.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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


internal typealias InstanceId = String

internal fun InstanceId?.tag(tag: String): String {
    return if (this.isNullOrBlank()) {
        tag
    } else {
        "$tag-$this"
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
internal class LivechatWidgetWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WebView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        const val TAG = "LcWidgetWebView"
        private const val DEFAULT_WIDGET_URI = "https://livechat.infobip.com/widget.js"
    }

    private val widgetPageUri = context.getString(R.string.ib_livechat_widget_page_uri)
    private val widgetUri = context.getString(R.string.ib_livechat_widget_uri)

    var livechatWidgetClient: LivechatWidgetClient? = null
        private set
    var instanceId: String? = null

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = LOAD_NO_CACHE
        }
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setup(
        webViewManager: LivechatWidgetWebViewManager,
        coroutineScope: CoroutineScope
    ) {
        webChromeClient = LivechatWidgetWebChromeClient(instanceId)
        webViewClient = LivechatWidgetWebViewClient(webViewManager, instanceId)
        addJavascriptInterface(LivechatWidgetJsInterfaceImpl(webViewManager, instanceId, coroutineScope), LivechatWidgetJsInterface.name)
        livechatWidgetClient = LivechatWidgetClientImpl(this, instanceId, coroutineScope)
    }

    fun loadWidgetPage(
        pushRegistrationId: String,
        widgetId: String,
        jwt: String? = null,
        domain: String? = null,
        widgetTheme: String? = null,
        language: String? = null
    ) {
        val builder = Uri.Builder()
            .encodedPath(widgetPageUri)
            .appendQueryParameter("pushRegId", pushRegistrationId)
            .appendQueryParameter("widgetId", widgetId)

        val encodedWidgetUri = widgetUri.takeIf { it != DEFAULT_WIDGET_URI }?.encode()

        if (widgetTheme?.isNotBlank() == true) {
            builder.appendQueryParameter("widgetTheme", widgetTheme)
        }

        if (language?.isNotBlank() == true) {
            builder.appendQueryParameter("language", language)
        }

        if (encodedWidgetUri?.isNotBlank() == true) {
            builder.appendQueryParameter("widgetUri", encodedWidgetUri)
        }

        if (domain?.isNotBlank() == true) {
            builder.appendQueryParameter("domain", domain)
        }

        if (jwt?.isNotBlank() == true) {
            builder.appendQueryParameter("jwt", jwt)
        }

        val resultUrl = builder.build().toString()
        MobileMessagingLogger.d(instanceId?.tag(TAG), "Loading page: $resultUrl")
        loadUrl(resultUrl)
    }

    private fun String.encode(): String = Uri.encode(this)

}