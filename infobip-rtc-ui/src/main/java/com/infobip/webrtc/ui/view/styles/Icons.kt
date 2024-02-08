package com.infobip.webrtc.ui.view.styles

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import com.infobip.webrtc.ui.R

data class Icons(
    @DrawableRes val mute: Int,
    @DrawableRes val unMute: Int,
    @DrawableRes val screenShare: Int,
    @DrawableRes val screenShareOff: Int,
    @DrawableRes val avatar: Int,
    @DrawableRes val video: Int,
    @DrawableRes val videoOff: Int,
    @DrawableRes val speaker: Int,
    @DrawableRes val speakerOff: Int,
    @DrawableRes val accept: Int,
    @DrawableRes val flipCamera: Int,
    @DrawableRes val endCall: Int,
    @DrawableRes val collapse: Int,
    @DrawableRes val decline: Int,
    @DrawableRes val callsIcon: Int,
    @DrawableRes val alertTriangle: Int,
) {

    companion object {
        internal operator fun invoke(
                context: Context,
                attrs: AttributeSet?
        ): Icons {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfobipRtcUi, R.attr.infobipRtcUiStyle, R.style.InfobipRtcUi)
            return typedArray.let {
                Icons(
                        mute = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_mute, R.drawable.ic_mute),
                        unMute = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_unMute, R.drawable.ic_unmute),
                        screenShare = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_screenShare, R.drawable.ic_screen_share),
                        screenShareOff = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_screenShareOff, R.drawable.ic_screen_share_off),
                        avatar = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_avatar, R.drawable.ic_user_grayscale),
                        video = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_video, R.drawable.ic_video),
                        videoOff = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_videoOff, R.drawable.ic_video_off),
                        speaker = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_speaker, R.drawable.ic_speaker),
                        speakerOff = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_speakerOff, R.drawable.ic_speaker_off),
                        accept = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_accept, R.drawable.ic_calls_30),
                        flipCamera = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_flipCamera, R.drawable.ic_flip_camera),
                        endCall = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_endCall, R.drawable.ic_endcall),
                        collapse = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_collapse, R.drawable.ic_collapse),
                        decline = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_decline, R.drawable.ic_clear_large),
                        callsIcon = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_callsIcon, R.drawable.ic_calls_30),
                        alertTriangle = it.getResourceId(R.styleable.InfobipRtcUi_rtc_ui_icon_alertTriangle, R.drawable.ic_alert_triangle),
                )
            }.also { typedArray.recycle() }
        }
    }
}