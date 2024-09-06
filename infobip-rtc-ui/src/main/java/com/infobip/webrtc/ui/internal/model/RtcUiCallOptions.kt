package com.infobip.webrtc.ui.internal.model

import com.infobip.webrtc.sdk.api.options.ApplicationCallOptions
import com.infobip.webrtc.sdk.api.options.AudioOptions
import com.infobip.webrtc.sdk.api.options.VideoOptions
import com.infobip.webrtc.sdk.api.options.WebrtcCallOptions

internal data class RtcUiCallOptions(
        val audio: Boolean = false,
        val video: Boolean = false,
        val audioOptions: AudioOptions? = null,
        val videoOptions: VideoOptions? = null,
        val customData: Map<String, String>? = null
) {
    fun toWebRtcCallOptions(): WebrtcCallOptions = WebrtcCallOptions.builder()
            .audioOptions(audioOptions ?: AudioOptions.builder().audioQualityMode(AudioOptions.AudioQualityMode.AUTO).build())
            .videoOptions(videoOptions ?: VideoOptions.builder().build())
            .customData(customData ?: mapOf())
            .audio(audio)
            .video(video)
            .build()

    fun toApplicationCallOptions(): ApplicationCallOptions = ApplicationCallOptions.builder()
            .audioOptions(audioOptions ?: AudioOptions.builder().audioQualityMode(AudioOptions.AudioQualityMode.AUTO).build())
            .videoOptions(videoOptions ?: VideoOptions.builder().build())
            .customData(customData ?: mapOf())
            .audio(audio)
            .video(video)
            .build()
}