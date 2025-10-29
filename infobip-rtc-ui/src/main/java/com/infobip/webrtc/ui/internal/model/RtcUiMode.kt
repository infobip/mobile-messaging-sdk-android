/*
 * RtcUiMode.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.model

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