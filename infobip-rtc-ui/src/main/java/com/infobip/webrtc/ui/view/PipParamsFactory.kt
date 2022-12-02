package com.infobip.webrtc.ui.view

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.fragments.InCallFragment.Companion.PIP_ACTION_HANGUP
import com.infobip.webrtc.ui.fragments.InCallFragment.Companion.PIP_ACTION_MUTE
import com.infobip.webrtc.ui.fragments.InCallFragment.Companion.PIP_ACTION_SPEAKER
import com.infobip.webrtc.ui.fragments.InCallFragment.Companion.PIP_ACTION_VIDEO
import com.infobip.webrtc.ui.fragments.InCallFragment.Companion.pipActionIntent

object PipParamsFactory {

    private const val REQUEST_MUTE = 1
    private const val REQUEST_SPEAKER = 2
    private const val REQUEST_FLIP_CAMERA = 3
    private const val REQUEST_HANGUP = 4
    private val intentFlag = intentFlag()

    private fun intentFlag() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        0 or PendingIntent.FLAG_IMMUTABLE
    else
        0

    private val rational = Rational(9, 16)

    @RequiresApi(Build.VERSION_CODES.O)
    fun createVideoPipParams(context: Context, isMuted: Boolean, hasLocalVideo: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(rational)
            .setActions(
                listOf(
                    RemoteAction(
                        Icon.createWithResource(context, if (isMuted) R.drawable.ic_unmute else R.drawable.ic_mute),
                        if (isMuted) context.getString(R.string.mm_unmute) else context.getString(R.string.mm_mute),
                        if (isMuted) context.getString(R.string.mm_unmute) else context.getString(R.string.mm_mute),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_MUTE,
                            pipActionIntent(PIP_ACTION_MUTE),
                            intentFlag
                        )
                    ),
                    RemoteAction(
                        Icon.createWithResource(context, if (hasLocalVideo) R.drawable.ic_video_off else R.drawable.ic_video),
                        context.getString(R.string.mm_video),
                        context.getString(R.string.mm_video),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_FLIP_CAMERA,
                            pipActionIntent(PIP_ACTION_VIDEO),
                            intentFlag
                        )
                    ),
                    RemoteAction(
                        Icon.createWithResource(context, R.drawable.ic_endcall),
                        context.getString(R.string.mm_hangup),
                        context.getString(R.string.mm_hangup),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_HANGUP,
                            pipActionIntent(PIP_ACTION_HANGUP),
                            intentFlag
                        )
                    )
                )
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createVoicePipParams(context: Context, isMuted: Boolean, isSpeakerOn: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(rational)
            .setActions(
                listOf(
                    RemoteAction(
                        Icon.createWithResource(context, if (isMuted) R.drawable.ic_unmute else R.drawable.ic_mute),
                        if (isMuted) context.getString(R.string.mm_unmute) else context.getString(R.string.mm_mute),
                        if (isMuted) context.getString(R.string.mm_unmute) else context.getString(R.string.mm_mute),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_MUTE,
                            pipActionIntent(PIP_ACTION_MUTE),
                            intentFlag
                        )
                    ),
                    RemoteAction(
                        Icon.createWithResource(context, if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_speaker_off),
                        context.getString(R.string.mm_speaker),
                        context.getString(R.string.mm_speaker),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_SPEAKER,
                            pipActionIntent(PIP_ACTION_SPEAKER),
                            intentFlag
                        )
                    ),
                    RemoteAction(
                        Icon.createWithResource(context, R.drawable.ic_endcall),
                        context.getString(R.string.mm_hangup),
                        context.getString(R.string.mm_hangup),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_HANGUP,
                            pipActionIntent(PIP_ACTION_HANGUP),
                            intentFlag
                        )
                    )
                )
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }
}