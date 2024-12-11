package com.infobip.webrtc.ui.model

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.internal.core.Injector

sealed class InCallButton(
    @StringRes open val labelRes: Int,
    @DrawableRes open val iconRes: Int,
    @DrawableRes open var checkedIconRes: Int? = null,
    open var onClick: (() -> Unit) = {}
) {
    abstract val id: Int

    internal object HangUp : InCallButton(
        R.string.mm_hangup,
        Injector.cache.icons?.endCall ?: R.drawable.ic_endcall,
        null
    ) {
        override val id: Int = R.id.rtc_ui_hang_up_button
    }

    data class Mute(override var onClick: () -> Unit = {}) : InCallButton(
        R.string.mm_microphone,
        Injector.cache.icons?.mic ?: R.drawable.ic_mic,
        Injector.cache.icons?.micOff ?: R.drawable.ic_mic_off,
    ) {
        override val id: Int = R.id.rtc_ui_mic_button
    }

    data class Speaker (override var onClick: () -> Unit = {})  : InCallButton(
        R.string.mm_speaker,
        Injector.cache.icons?.speakerOff ?: R.drawable.ic_speaker_off,
        Injector.cache.icons?.speaker ?: R.drawable.ic_speaker,
    ) {
        override val id: Int = R.id.rtc_ui_speaker_button
    }

    data class FlipCam (override var onClick: () -> Unit = {})  : InCallButton(
        R.string.mm_flip_camera,
        Injector.cache.icons?.flipCamera ?: R.drawable.ic_flip_camera
    ) {
        override val id: Int = R.id.rtc_ui_flip_cam_button
    }

    data class ScreenShare (override var onClick: () -> Unit = {})  : InCallButton(
        R.string.mm_screen_share,
        Injector.cache.icons?.screenShare ?: R.drawable.ic_screen_share,
        Injector.cache.icons?.screenShareOff ?: R.drawable.ic_screen_share_off,
    ) {
        override val id: Int = R.id.rtc_ui_screen_share_button
    }

    data class Video (override var onClick: () -> Unit = {})  : InCallButton(
        R.string.mm_video_call,
        Injector.cache.icons?.videoOff ?: R.drawable.ic_video_off,
        Injector.cache.icons?.video ?: R.drawable.ic_video,
    ) {
        override val id: Int = R.id.rtc_ui_video_button
    }

    data class Custom(
        @StringRes override val labelRes: Int,
        @DrawableRes override val iconRes: Int,
        @DrawableRes override var checkedIconRes: Int? = null,
        override var onClick: () -> Unit,
        val setChecked: (() -> Boolean)? = null,
        val setEnabled: (() -> Boolean)? = null
    ) : InCallButton(labelRes, iconRes) {
        override val id: Int = View.generateViewId()
    }
}
