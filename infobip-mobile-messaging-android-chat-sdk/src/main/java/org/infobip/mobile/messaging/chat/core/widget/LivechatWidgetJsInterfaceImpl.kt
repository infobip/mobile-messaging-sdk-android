package org.infobip.mobile.messaging.chat.core.widget

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

/**
 * Implementation of [LivechatWidgetJsInterface]
 * Low level communication from JS Livechat Widget to native code.
 */
internal class LivechatWidgetJsInterfaceImpl(
    private val widgetWebViewManager: LivechatWidgetWebViewManager,
    private val coroutineScope: CoroutineScope,
) : LivechatWidgetJsInterface {

    companion object {
        private const val TAG = "LivechatWidgetJsInterface"
    }

    @JavascriptInterface
    override fun setControlsVisibility(isVisible: Boolean) {
        coroutineScope.launch(Dispatchers.Main) {
            widgetWebViewManager.setControlsVisibility(isVisible)
        }
    }

    @JavascriptInterface
    override fun openAttachmentPreview(url: String?, type: String?, caption: String?) {
        coroutineScope.launch(Dispatchers.Main) {
            widgetWebViewManager.openAttachmentPreview(url, type, caption)
        }
    }

    @JavascriptInterface
    override fun onViewChanged(view: String?) {
        coroutineScope.launch(Dispatchers.Main) {
            MobileMessagingLogger.d(TAG, "Widget onWidgetViewChanged: $view")
            runCatching {
                widgetWebViewManager.onWidgetViewChanged(LivechatWidgetView.valueOf(view.orEmpty()))
            }.onFailure {
                MobileMessagingLogger.e("Could not parse LivechatWidgetView from $view", it)
            }
        }
    }

    @JavascriptInterface
    override fun onRawMessageReceived(message: String?) {
        coroutineScope.launch(Dispatchers.Main) {
            widgetWebViewManager.onWidgetRawMessageReceived(message)
        }
    }

    @JavascriptInterface
    override fun onWidgetApiError(method: String?, errorPayload: String?) {
        coroutineScope.launch(Dispatchers.Main) {
            val result = if (errorPayload?.isNotBlank() == true) " => $errorPayload" else ""
            MobileMessagingLogger.e(TAG, "Widget API call error: $method()$result")
            runCatching {
                widgetWebViewManager.onWidgetApiError(LivechatWidgetMethod.valueOf(method.orEmpty()), errorPayload)
            }.onFailure {
                MobileMessagingLogger.e(TAG, "Could not parse WidgetMethod from $method", it)
            }
        }
    }

    @JavascriptInterface
    override fun onWidgetApiSuccess(method: String?, successPayload: String?) {
        coroutineScope.launch(Dispatchers.Main) {
            var payload: String? = successPayload
            if (payload?.isNotBlank() == true && payload.startsWith("\"") && payload.endsWith("\"") && payload.length > 2) {
                payload = payload.substring(1, payload.length - 1)
            }
            payload = payload?.let { LivechatWidgetClientImpl.shortenLog(it) }
            val result = if (payload?.isNotBlank() == true) " => $payload" else ""
            MobileMessagingLogger.d(TAG, "Widget API call result: $method()$result")
            runCatching {
                widgetWebViewManager.onWidgetApiSuccess(LivechatWidgetMethod.valueOf(method.orEmpty()), payload)
            }.onFailure {
                MobileMessagingLogger.e(TAG, "Could not parse WidgetMethod from $method", it)
            }
        }
    }

}