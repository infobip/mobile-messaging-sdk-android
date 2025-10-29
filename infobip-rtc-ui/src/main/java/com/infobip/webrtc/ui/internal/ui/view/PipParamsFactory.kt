/*
 * PipParamsFactory.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.ui.view

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.internal.ui.fragment.InCallFragment.Companion.PIP_ACTION_HANGUP
import com.infobip.webrtc.ui.internal.ui.fragment.InCallFragment.Companion.PIP_ACTION_MUTE
import com.infobip.webrtc.ui.internal.ui.fragment.InCallFragment.Companion.PIP_ACTION_SPEAKER
import com.infobip.webrtc.ui.internal.ui.fragment.InCallFragment.Companion.PIP_ACTION_VIDEO
import com.infobip.webrtc.ui.internal.ui.fragment.InCallFragment.Companion.pipActionIntent

internal object PipParamsFactory {

    private const val REQUEST_MUTE = 1
    private const val REQUEST_UNMUTE = 2
    private const val REQUEST_SPEAKER_ON = 3
    private const val REQUEST_SPEAKER_OFF = 4
    private const val REQUEST_VIDEO_OFF = 5
    private const val REQUEST_HANGUP = 6
    private val intentFlag = intentFlag()

    private fun intentFlag() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        0 or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    else
        0

    private val rational = Rational(9, 16)

    @RequiresApi(Build.VERSION_CODES.O)
    fun createVideoPipParams(context: Context, isMuted: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(rational)
            .setActions(
                listOf(
                    RemoteAction(
                        Icon.createWithResource(context, if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic),
                        if (isMuted) context.getString(R.string.mm_unmute) else context.getString(R.string.mm_mute),
                        if (isMuted) context.getString(R.string.mm_unmute) else context.getString(R.string.mm_mute),
                        PendingIntent.getBroadcast(
                            context,
                            if (isMuted) REQUEST_UNMUTE else REQUEST_MUTE,
                            pipActionIntent(context, PIP_ACTION_MUTE),
                            intentFlag
                        )
                    ),
                    RemoteAction(
                        Icon.createWithResource(context, R.drawable.ic_video_off),
                        context.getString(R.string.mm_video),
                        context.getString(R.string.mm_video),
                        PendingIntent.getBroadcast(
                            context,
                            REQUEST_VIDEO_OFF ,
                            pipActionIntent(context, PIP_ACTION_VIDEO),
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
                            pipActionIntent(context, PIP_ACTION_HANGUP),
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
                        Icon.createWithResource(context, if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic),
                        if (isMuted) context.getString(R.string.mm_unmute) else context.getString(R.string.mm_mute),
                        if (isMuted) context.getString(R.string.mm_unmute) else context.getString(R.string.mm_mute),
                        PendingIntent.getBroadcast(
                            context,
                            if (isMuted) REQUEST_UNMUTE else REQUEST_MUTE,
                            pipActionIntent(context, PIP_ACTION_MUTE),
                            intentFlag
                        )
                    ),
                    RemoteAction(
                        Icon.createWithResource(context, if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_speaker_off),
                        context.getString(R.string.mm_speaker),
                        context.getString(R.string.mm_speaker),
                        PendingIntent.getBroadcast(
                            context,
                            if (isSpeakerOn) REQUEST_SPEAKER_OFF else REQUEST_SPEAKER_ON,
                            pipActionIntent(context, PIP_ACTION_SPEAKER),
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
                            pipActionIntent(context, PIP_ACTION_HANGUP),
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