package com.infobip.webrtc.ui.view.styles

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import com.infobip.webrtc.ui.R

data class Icons @JvmOverloads constructor(
    @DrawableRes val mic: Int = Defaults.mic,
    @DrawableRes val micOff: Int = Defaults.micOff,
    @DrawableRes val screenShare: Int = Defaults.screenShare,
    @DrawableRes val screenShareOff: Int = Defaults.screenShareOff,
    @DrawableRes val video: Int = Defaults.video,
    @DrawableRes val videoOff: Int = Defaults.videoOff,
    @DrawableRes val speaker: Int = Defaults.speaker,
    @DrawableRes val speakerOff: Int = Defaults.speakerOff,
    @DrawableRes val flipCamera: Int = Defaults.flipCamera,
    @DrawableRes val collapse: Int = Defaults.collapse,
    @DrawableRes val accept: Int = Defaults.accept,
    @DrawableRes val decline: Int = Defaults.decline,
    @DrawableRes val endCall: Int = Defaults.endCall,
    @DrawableRes val avatar: Int = Defaults.avatar,
    @DrawableRes val caller: Int = Defaults.accept,
    @DrawableRes val micOffAlertIcon: Int = Defaults.micOff,
    @DrawableRes val weakConnectionAlertIcon: Int = Defaults.alertTriangle,
    @DrawableRes val reconnectingAlertIcon: Int = Defaults.alertTriangle,
) {

    object Defaults {
        @DrawableRes
        val mic = R.drawable.ic_mic
        @DrawableRes
        val micOff = R.drawable.ic_mic_off
        @DrawableRes
        val screenShare = R.drawable.ic_screen_share
        @DrawableRes
        val screenShareOff = R.drawable.ic_screen_share_off
        @DrawableRes
        val video = R.drawable.ic_video
        @DrawableRes
        val videoOff = R.drawable.ic_video_off
        @DrawableRes
        val speaker = R.drawable.ic_speaker
        @DrawableRes
        val speakerOff = R.drawable.ic_speaker_off
        @DrawableRes
        val flipCamera = R.drawable.ic_flip_camera
        @DrawableRes
        val collapse = R.drawable.ic_collapse
        @DrawableRes
        val accept = R.drawable.ic_calls_30
        @DrawableRes
        val decline = R.drawable.ic_clear_large
        @DrawableRes
        val endCall = R.drawable.ic_endcall
        @DrawableRes
        val avatar = R.drawable.ic_user_grayscale
        @DrawableRes
        val alertTriangle = R.drawable.ic_alert_triangle
    }

    companion object {
        internal operator fun invoke(
            context: Context,
            attrs: AttributeSet?
        ): Icons {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfobipRtcUi, R.attr.infobipRtcUiStyle, R.style.InfobipRtcUi)
            return typedArray.let {
                Icons(
                    mic = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_mic, Defaults.mic),
                    micOff = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_micOff, Defaults.micOff),
                    screenShare = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_screenShare, Defaults.screenShare),
                    screenShareOff = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_screenShareOff, Defaults.screenShareOff),
                    video = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_video, Defaults.video),
                    videoOff = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_videoOff, Defaults.videoOff),
                    speaker = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_speaker, Defaults.speaker),
                    speakerOff = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_speakerOff, Defaults.speakerOff),
                    flipCamera = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_flipCamera, Defaults.flipCamera),
                    collapse = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_collapse, Defaults.collapse),
                    accept = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_accept, Defaults.accept),
                    decline = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_decline, Defaults.decline),
                    endCall = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_endCall, Defaults.endCall),
                    avatar = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_avatar, Defaults.avatar),
                    caller = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_caller, Defaults.accept),
                    micOffAlertIcon = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_alert_mic_off, Defaults.micOff),
                    weakConnectionAlertIcon = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_alert_weak_connection, Defaults.alertTriangle),
                    reconnectingAlertIcon = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_alert_reconnecting, Defaults.alertTriangle),
                )
            }.also { typedArray.recycle() }
        }
    }
}