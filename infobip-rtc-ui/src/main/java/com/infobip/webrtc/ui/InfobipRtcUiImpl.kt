package com.infobip.webrtc.ui

import android.content.Context
import android.util.Log
import com.infobip.webrtc.Cache
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.TokenProvider
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.sdk.api.model.push.Status
import com.infobip.webrtc.ui.delegate.CallsDelegate
import com.infobip.webrtc.ui.delegate.NotificationPermissionDelegate
import com.infobip.webrtc.ui.delegate.PushIdDelegate
import com.infobip.webrtc.ui.model.ListenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

internal class InfobipRtcUiImpl(
        private val context: Context,
        private val tokenProvider: TokenProvider,
        private val cache: Cache,
        private val callsDelegate: CallsDelegate,
        private val callsScope: CoroutineScope,
        private val pushIdDelegate: PushIdDelegate,
        private val rtcInstance: InfobipRTC,
        private val notificationPermissionDelegate: NotificationPermissionDelegate,
) : InfobipRtcUi {

    override fun enableCalls(identity: String, listenType: ListenType, successListener: SuccessListener?, errorListener: ErrorListener?) {
        Injector.enableInAppCallsSuccess = successListener
        Injector.enableInAppCallsError = errorListener
        callsScope.launch {
            runCatching {
                cache.identity = identity
                tokenProvider.getToken(identity, cache.applicationId)?.let { token ->
                    if (listenType == ListenType.PUSH) {
                        if (notificationPermissionDelegate.isPermissionNeeded()) {
                            withContext(Dispatchers.Main) {
                                notificationPermissionDelegate.request()
                            }
                        }
                        registerPush(token, errorListener, successListener)
                    } else {
                        registerActiveConnection(token, errorListener, successListener)
                    }
                } ?: runOnUiThread {
                    errorListener?.onError(IllegalStateException("Could not create WebRTC token."))
                    cleanStoredCallbacks()
                }
            }.onFailure {
                runOnUiThread { errorListener?.onError(it) }
                cleanStoredCallbacks()
            }
        }
    }

    override fun enableInAppCalls(successListener: SuccessListener?, errorListener: ErrorListener?) {
        cache.inAppCallsEnabled = true
        pushIdDelegate.getPushRegistrationId()?.let { pushRegId ->
            enableCalls(
                    identity = pushRegId,
                    listenType = ListenType.PUSH,
                    successListener = successListener,
                    errorListener = errorListener
            )
        } ?: Log.d(TAG, "Could not obtain push registration ID, waiting for broadcast.")
    }

    override fun disableCalls(successListener: SuccessListener?, errorListener: ErrorListener?) {
        runCatching {
            val identity = cache.identity
            require(identity.isNotEmpty()) {
                "Calls are not registered."
            }
            callsScope.launch {
                rtcInstance.disablePushNotification(tokenProvider.getToken(identity, cache.applicationId), context)
                cache.clear()
                callsScope.coroutineContext.cancelChildren()
            }
        }.onSuccess {
            successListener?.onSuccess()
        }.onFailure {
            errorListener?.onError(it)
        }

    }

    override fun setLanguage(locale: Locale) {
        Injector.locale = locale
    }

    private fun registerPush(token: String, errorListener: ErrorListener?, successListener: SuccessListener?) {
        callsDelegate.enablePush(token) {
            Log.d(TAG, "Registration for calls push result: ${it.status}, ${it.description}")
            runOnUiThread {
                if (it.status != Status.SUCCESS) {
                    errorListener?.onError(IllegalStateException("Registration for calls push failed. Reason: ${it.description}"))
                } else {
                    successListener?.onSuccess()
                }
                cleanStoredCallbacks()
            }
        }
    }

    private fun registerActiveConnection(token: String, errorListener: ErrorListener?, successListener: SuccessListener?) {
        runCatching {
            callsDelegate.registerActiveConnection(token)
        }.onSuccess {
            runOnUiThread { successListener?.onSuccess() }
        }.onFailure {
            runOnUiThread { errorListener?.onError(it) }
        }
        cleanStoredCallbacks()
    }

    private fun runOnUiThread(action: () -> Unit) {
        callsScope.launch(Dispatchers.Main) {
            action()
        }
    }

    private fun cleanStoredCallbacks() {
        with(Injector) {
            enableInAppCallsError = null
            enableInAppCallsSuccess = null
        }
    }
}


