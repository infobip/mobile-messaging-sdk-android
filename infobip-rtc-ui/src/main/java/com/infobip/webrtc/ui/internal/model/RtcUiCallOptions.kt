/*
 * RtcUiCallOptions.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.model

import com.infobip.webrtc.sdk.api.options.ApplicationCallOptions
import com.infobip.webrtc.sdk.api.options.AudioOptions
import com.infobip.webrtc.sdk.api.options.VideoOptions
import com.infobip.webrtc.sdk.api.options.WebrtcCallOptions

internal sealed class RtcUiCallOptions {

    abstract val audio: Boolean
    abstract val video: Boolean
    abstract val autoReconnect: Boolean
    abstract val dataChannel: Boolean
    abstract val audioOptions: AudioOptions
    abstract val videoOptions: VideoOptions
    abstract val customData: Map<String, String>

    data class WebRtcCall(
        val options: WebrtcCallOptions
    ) : RtcUiCallOptions() {
        override val audio: Boolean
            get() = options.isAudio
        override val video: Boolean
            get() = options.isVideo
        override val autoReconnect: Boolean
            get() = options.isAutoReconnect
        override val dataChannel: Boolean
            get() = options.isDataChannel
        override val audioOptions: AudioOptions
            get() = options.audioOptions
        override val videoOptions: VideoOptions
            get() = options.videoOptions
        override val customData: Map<String, String>
            get() = options.customData
    }

    data class ApplicationCall(
        val options: ApplicationCallOptions
    ) : RtcUiCallOptions() {
        override val audio: Boolean
            get() = options.isAudio
        override val video: Boolean
            get() = options.isVideo
        override val autoReconnect: Boolean
            get() = options.isAutoReconnect
        override val dataChannel: Boolean
            get() = options.isDataChannel
        override val audioOptions: AudioOptions
            get() = options.audioOptions
        override val videoOptions: VideoOptions
            get() = options.videoOptions
        override val customData: Map<String, String>
            get() = options.customData
    }

}