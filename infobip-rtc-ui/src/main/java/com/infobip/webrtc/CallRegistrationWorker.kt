package com.infobip.webrtc

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.infobip.webrtc.ui.InfobipRtcUi
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.model.ListenType
import com.infobip.webrtc.ui.model.RtcUiMode
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class CallRegistrationWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val ARG_IDENTITY: String = "com.infobip.webrtc.CallRegistrationWorker.IDENTITY"
        private const val ARG_DISABLE_PREVIOUS_IDENTITY: String = "com.infobip.webrtc.CallRegistrationWorker.ARG_DISABLE_PREVIOUS_IDENTITY"
        private const val MAX_ATTEMPT_COUNT: Int = 3
        private const val TAG = "CallRegistrationWorker"
        private const val CALL_REGISTRATION_SERVICE_CHANNEL_ID = "com.infobip.webrtc.CallRegistrationWorker.CALL_REGISTRATION_SERVICE_CHANNEL_ID"
        private const val NOTIFICATION_ID = 2001

        fun launch(
            context: Context,
            identity: String?,
            disablePreviousIdentity: Boolean = false
        ) {
            if (identity?.isNotBlank() == true) {
                val work = OneTimeWorkRequestBuilder<CallRegistrationWorker>().apply {
                    setInputData(
                        Data.Builder()
                            .putString(ARG_IDENTITY, identity)
                            .putBoolean(ARG_DISABLE_PREVIOUS_IDENTITY, disablePreviousIdentity)
                            .build()
                    )
                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        10,
                        TimeUnit.SECONDS
                    )
                }.build()
                WorkManager.getInstance(context).enqueue(work)
            }
        }
    }

    override suspend fun doWork(): Result {
        val webrtcUi = Injector.getWebrtcUi(applicationContext)
        val identity = params.inputData.getString(ARG_IDENTITY)
        val previousIdentity = Injector.cache.identity

        if (previousIdentity.isNotBlank() && previousIdentity == identity)
            return Result.success()

        return if (identity == null) {
            Result.failure()
        } else {
            val disablePreviousIdentity = params.inputData.getBoolean(ARG_DISABLE_PREVIOUS_IDENTITY, false)
            if (disablePreviousIdentity && previousIdentity.isNotBlank())
                webrtcUi.disableCallsForPreviousIdentity()
            suspendCancellableCoroutine { cont ->
                val mode = Injector.cache.rtcUiMode?.takeIf { it == RtcUiMode.DEFAULT || it == RtcUiMode.IN_APP_CHAT }
                val successListener = mode?.successListener
                val errorListener = mode?.errorListener
                webrtcUi.enableCalls(
                    identity = identity,
                    listenType = ListenType.PUSH,
                    successListener = {
                        successListener?.onSuccess()
                        Log.d(TAG, "$mode calls enabled from broadcast. ")
                        if (cont.isActive)
                            cont.resume(Result.success())
                    },
                    errorListener = {
                        errorListener?.onError(it)
                        Log.e(TAG, "Failed to enabled $mode calls from broadcast.", it)
                        if (runAttemptCount <= MAX_ATTEMPT_COUNT) {
                            Log.d(TAG, "Exception occurred, attempt: $runAttemptCount/$MAX_ATTEMPT_COUNT")
                            if (cont.isActive)
                                cont.resume(Result.retry())
                        } else {
                            Log.d(TAG, "Max attempt reached, return Failure")
                            if (cont.isActive)
                                cont.resume(Result.failure())
                        }
                    }
                )
            }
        }
    }

    private suspend fun InfobipRtcUi.disableCallsForPreviousIdentity() = coroutineScope {
        val mode = Injector.cache.rtcUiMode
        val onResultAction: (CancellableContinuation<Unit>, String) -> Unit = { cont, msg ->
            Log.d(TAG, msg)
            if (cont.isActive)
                cont.resume(Unit)
        }
        suspendCancellableCoroutine { cont ->
            disableCalls(
                { onResultAction(cont, "Calls disabled.") },
                { onResultAction(cont, "Failed to disable calls.") }
            )
        }
        Injector.cache.rtcUiMode = mode
        delay(2000L) //Delay gives WebRTC BE time to process action
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            createNotification(context.getString(R.string.mm_call_registration_notification_description))
        )
    }

    private fun createNotification(title: String): Notification {
        val builder = NotificationCompat.Builder(context, CALL_REGISTRATION_SERVICE_CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_screen_share)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                CALL_REGISTRATION_SERVICE_CHANNEL_ID,
                context.getString(R.string.mm_call_registration_notification_channel)
            ).also {
                builder.setChannelId(it.id)
            }
        }
        return builder.build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String,
        name: String
    ): NotificationChannel {
        return NotificationChannel(
            channelId, name, NotificationManager.IMPORTANCE_LOW
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }
}