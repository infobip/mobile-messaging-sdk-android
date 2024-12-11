package com.infobip.webrtc.ui.internal.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.Person
import androidx.core.content.getSystemService
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.service.OngoingCallService
import com.infobip.webrtc.ui.internal.ui.CallActivity
import com.infobip.webrtc.ui.internal.utils.resolveStyledStringAttribute

const val CALL_NOTIFICATION_ID = 9999
const val SCREEN_SHARE_NOTIFICATION_ID = 9998

internal interface CallNotificationFactory {
    fun createIncomingCallNotification(
        context: Context,
        callerName: String,
        description: String,
    ): Notification

    fun createIncomingCallNotificationSilent(
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

    private fun createIncomingCallNotification(
        context: Context,
        callerName: String,
        description: String,
        isSilent: Boolean,
    ): Notification {
        val themedContext by lazy { ContextThemeWrapper(context, R.style.InfobipRtcUi_Call) }
        val incomingCallScreenMessage: String? = Injector.cache.incomingCallScreenStyle?.messageText ?: themedContext.resolveStyledStringAttribute(
            R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message,
            R.attr.infobipRtcUiStyle,
            R.styleable.InfobipRtcUi
        )
        val incomingCallScreenHeadline: String? = Injector.cache.incomingCallScreenStyle?.headlineText ?: themedContext.resolveStyledStringAttribute(
            R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline,
            R.attr.infobipRtcUiStyle,
            R.styleable.InfobipRtcUi
        )
        val incomingCallScreenCallerName: String? = Injector.cache.incomingCallScreenStyle?.callerName ?: themedContext.resolveStyledStringAttribute(
            R.styleable.InfobipRtcUi_rtc_ui_incoming_call_caller_name,
            R.attr.infobipRtcUiStyle,
            R.styleable.InfobipRtcUi
        )
        val acceptCall = incomingCallScreenMessage.isNullOrEmpty() && incomingCallScreenHeadline.isNullOrEmpty()
        val displayName = incomingCallScreenCallerName ?: callerName

        val acceptIntent = PendingIntent.getActivity(
            context, CALL_ACCEPT_REQUEST_CODE,
            CallActivity.newInstance(context, callerName, acceptCall),
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

        return commonCallNotification(displayName, description, INCOMING_CALL_NOTIFICATION_CHANNEL_ID) {
            foregroundServiceBehavior = FOREGROUND_SERVICE_IMMEDIATE
            setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    Person.Builder().setName(displayName).setImportant(true).build(),
                    declineIntent,
                    acceptIntent
                )
            )
            setSilent(isSilent)
            priority = NotificationCompat.PRIORITY_MAX

            var hasFullScreenIntentPermission = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                hasFullScreenIntentPermission = notificationManager.canUseFullScreenIntent()
            }
            val intent = contentIntent(callerName)
            if (hasFullScreenIntentPermission)
                setFullScreenIntent(intent, true)
            else
                setContentIntent(intent)
        }
    }

    override fun createIncomingCallNotification(
        context: Context,
        callerName: String,
        description: String
    ): Notification {
        return createIncomingCallNotification(context, callerName, description, false)
    }

    override fun createIncomingCallNotificationSilent(
        context: Context,
        callerName: String,
        description: String
    ): Notification {
        return createIncomingCallNotification(context, callerName, description, true)
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

        return commonCallNotification(title, description, IN_CALL_NOTIFICATION_CHANNEL_ID) {
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(contentIntent(title))
            setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    Person.Builder().setName(title).setImportant(true).build(),
                    hangupIntent
                )
            )
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