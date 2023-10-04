package com.infobip.webrtc.ui.model

import com.infobip.webrtc.ui.ErrorListener
import com.infobip.webrtc.ui.SuccessListener

internal enum class RtcUiMode(
    var successListener: SuccessListener? = null,
    var errorListener: ErrorListener? = null
) {
    CUSTOM,
    DEFAULT,
    IN_APP_CHAT;

    fun withListeners(
        successListener: SuccessListener?,
        errorListener: ErrorListener?
    ): RtcUiMode = this.also {
        this.successListener = successListener
        this.errorListener = errorListener
    }

    fun cleanListeners(): RtcUiMode = this.also {
        this.successListener = null
        this.errorListener = null
    }
}