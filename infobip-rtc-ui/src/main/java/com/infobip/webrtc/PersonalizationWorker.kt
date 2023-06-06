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
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.model.ListenType
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class PersonalizationWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val PUSH_REG_ID: String =
            "com.infobip.webrtc.PersonalizationWorker.PUSH_REG_ID"
        private const val MAX_ATTEMPT_COUNT: Int = 3
        private const val TAG = "PersonalizationWorker"
        private const val PERSONALIZATION_SERVICE_CHANNEL_ID =
            "com.infobip.webrtc.PersonalizationWorker.PERSONALIZATION_SERVICE_CHANNEL_ID"
        private const val NOTIFICATION_ID = 2001

        fun launch(context: Context, pushRegId: String?) {
            if (pushRegId?.isNotBlank() == true) {
                val work = OneTimeWorkRequestBuilder<PersonalizationWorker>().apply {
                    setInputData(Data.Builder().putString(PUSH_REG_ID, pushRegId).build())
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
        return suspendCancellableCoroutine { cont ->
            params.inputData.getString(PUSH_REG_ID)?.let { pushRegId ->
                val successListener = Injector.enableInAppCallsSuccess
                val errorListener = Injector.enableInAppCallsError
                Injector.getWebrtcUi(applicationContext).enableCalls(
                    identity = pushRegId,
                    listenType = ListenType.PUSH,
                    successListener = {
                        successListener?.onSuccess()
                        Log.d(TAG, "InAppCalls enabled from broadcast.")
                        if (cont.isActive)
                            cont.resume(Result.success())
                    },
                    errorListener = {
                        errorListener?.onError(it)
                        Log.e(TAG, "Failed to enabled InAppCalls from broadcast.", it)
                        if (runAttemptCount <= MAX_ATTEMPT_COUNT) {
                            Log.d(TAG, "Exception occurred, return Retry x$runAttemptCount")
                            if (cont.isActive)
                                cont.resume(Result.retry())
                        } else {
                            Log.d(TAG, "Max attempt reached, return Failure")
                            if (cont.isActive)
                                cont.resume(Result.failure())
                        }
                    }
                )
            } ?: cont.apply {
                if (isActive)
                    resume(Result.failure())
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            createNotification(context.getString(R.string.mm_personalization_notification_description))
        )
    }

    private fun createNotification(title: String): Notification {
        val builder = NotificationCompat.Builder(context, PERSONALIZATION_SERVICE_CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_screen_share)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                PERSONALIZATION_SERVICE_CHANNEL_ID,
                context.getString(R.string.mm_personalization_notification_channel)
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