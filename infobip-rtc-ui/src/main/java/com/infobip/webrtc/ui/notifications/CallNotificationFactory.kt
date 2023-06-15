package com.infobip.webrtc.ui.notifications

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.infobip.webrtc.Injector
import com.infobip.webrtc.ui.CallActivity
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.service.OngoingCallService
import com.infobip.webrtc.ui.utils.applyLocale
import kotlin.reflect.KProperty

const val CALL_NOTIFICATION_ID = 9999
const val SCREEN_SHARE_NOTIFICATION_ID = 9998

interface CallNotificationFactory {
    fun createIncomingCallNotification(
        context: Context,
        callerName: String,
        description: String,
    ): Notification

    fun createOngoingCallNotification(
        context: Context,
        title: String,
        description: String
    ): Notification

    fun createScreenSharingNotification(
        context: Context,
    ): Notification
}


internal class CallNotificationFactoryImpl(
    private val context: Context
) : CallNotificationFactory {

    companion object {
        private const val INCOMING_CALL_NOTIFICATION_CHANNEL_ID =
            "com.infobip.conversations.app.INCOMING_CALL_NOTIFICATION_CHANNEL_ID"
        private const val IN_CALL_NOTIFICATION_CHANNEL_ID =
            "com.infobip.conversations.app.IN_CALL_NOTIFICATION_CHANNEL_ID"
        private const val SCREEN_SHARING_NOTIFICATION_CHANNEL_ID =
            "com.infobip.conversations.app.SCREEN_SHARING_NOTIFICATION_CHANNEL_ID"
        private const val CALL_NOTIFICATION_CONTENT_REQUEST_CODE = 8999
        private const val CALL_DECLINE_REQUEST_CODE = 8899
        private const val CALL_ACCEPT_REQUEST_CODE = 8889
        private const val CALL_HANGUP_REQUEST_CODE = 8888

    }

    private val notificationManager: NotificationManager =
        context.getSystemService()
            ?: throw IllegalStateException("Could not get system service: NotificationManager")
    private val updateCurrentImmutableFlags = PendingIntent.FLAG_UPDATE_CURRENT.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            it or PendingIntent.FLAG_IMMUTABLE
        else
            it
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                context.getString(R.string.mm_calls_notification_channel),
                INCOMING_CALL_NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_HIGH
            )
            createNotificationChannel(
                context.getString(R.string.mm_in_call_notification_channel),
                IN_CALL_NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            createNotificationChannel(
                context.getString(R.string.mm_screen_sharing),
                SCREEN_SHARING_NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(name: String, channelId: String, importance: Int) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelId,
                name,
                importance
            ).apply {
                description = name
                enableLights(true)
                this.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                this.setShowBadge(false)
            })
    }

    override fun createIncomingCallNotification(
        context: Context,
        callerName: String,
        description: String,
    ): Notification {
        val acceptIntent = PendingIntent.getActivity(
            context, CALL_ACCEPT_REQUEST_CODE,
            CallActivity.newInstance(context, callerName, true),
            updateCurrentImmutableFlags
        )
        val declineIntent = PendingIntent.getService(
            context,
            CALL_DECLINE_REQUEST_CODE,
            Intent(context, OngoingCallService::class.java).apply {
                action = OngoingCallService.CALL_DECLINED_ACTION
            },
            updateCurrentImmutableFlags
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Notification.Builder(context, INCOMING_CALL_NOTIFICATION_CHANNEL_ID)
                .setFullScreenIntent(contentIntent(callerName), true)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .setSmallIcon(R.drawable.ic_calls_30)
                .setStyle(
                    Notification.CallStyle.forIncomingCall(
                        Person.Builder().setName(callerName).setImportant(true).build(),
                        declineIntent,
                        acceptIntent
                    )
                )
                .build()
        else {
            commonCallNotification(callerName, description, INCOMING_CALL_NOTIFICATION_CHANNEL_ID) {
                setPriority(NotificationCompat.PRIORITY_MAX)
                    .addAction(
                        R.drawable.ic_calls_30,
                        context.getString(R.string.mm_accept),
                        acceptIntent
                    )
                    .addAction(
                        R.drawable.ic_endcall,
                        context.getString(R.string.mm_decline),
                        declineIntent
                    )
                    .setFullScreenIntent(contentIntent(callerName), true)
                    .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            }
        }
    }

    override fun createOngoingCallNotification(
        context: Context,
        title: String,
        description: String
    ): Notification {
        val hangupIntent = PendingIntent.getService(
            context,
            CALL_HANGUP_REQUEST_CODE,
            Intent(context, OngoingCallService::class.java).apply {
                action = OngoingCallService.CALL_HANGUP_ACTION
            },
            updateCurrentImmutableFlags
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Notification.Builder(context, IN_CALL_NOTIFICATION_CHANNEL_ID)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(contentIntent(title))
                .setSmallIcon(R.drawable.ic_calls_30)
                .setStyle(
                    Notification.CallStyle.forOngoingCall(
                        Person.Builder().setName(title).setImportant(true).build(),
                        hangupIntent
                    )
                ).build()
        else
            commonCallNotification(title, description, IN_CALL_NOTIFICATION_CHANNEL_ID) {
                setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(contentIntent(title))
                    .addAction(
                        R.drawable.ic_endcall,
                        context.getString(R.string.mm_hangup),
                        hangupIntent
                    )
                    .setColor(ContextCompat.getColor(context, R.color.rtc_ui_notification))
                    .setColorized(true)
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            }
    }

    override fun createScreenSharingNotification(
        context: Context,
    ): Notification {
        return commonCallNotification(
            context.getString(R.string.mm_screen_share),
            context.getString(R.string.mm_screen_sharing_description),
            SCREEN_SHARING_NOTIFICATION_CHANNEL_ID
        )
    }

    private fun commonCallNotification(
        title: String,
        description: String,
        channelId: String,
        modify: NotificationCompat.Builder.() -> NotificationCompat.Builder = { this }
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_calls_30)
            .setContentTitle(title)
            .setContentText(description)
            .let(modify)
            .build()
    }

    private fun contentIntent(title: String): PendingIntent {
        val activityClass = Injector.cache.activityClass
        val contentIntent = if (activityClass == CallActivity::class.java)
            CallActivity.newInstance(context, caller = title)
        else
            Intent(context, activityClass)

        return PendingIntent.getActivity(
            context,
            CALL_NOTIFICATION_CONTENT_REQUEST_CODE,
            contentIntent,
            updateCurrentImmutableFlags
        )
    }
}