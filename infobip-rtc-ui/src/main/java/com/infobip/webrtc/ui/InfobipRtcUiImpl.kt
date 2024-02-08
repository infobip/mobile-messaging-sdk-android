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
import com.infobip.webrtc.ui.model.InCallButton
import com.infobip.webrtc.ui.model.ListenType
import com.infobip.webrtc.ui.model.RtcUiMode
import com.infobip.webrtc.ui.view.styles.InfobipRtcUiTheme
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

    private var rtcUiMode: RtcUiMode?
        get() = cache.rtcUiMode
        set(value) {
            cache.rtcUiMode = value
        }

    override fun enableCalls(identity: String, listenType: ListenType, successListener: SuccessListener?, errorListener: ErrorListener?) {
        //it can be called also from broadcast receivers when we already have mode
        if (rtcUiMode == null)
            rtcUiMode = RtcUiMode.CUSTOM.withListeners(successListener, errorListener)
        callsScope.launch {
            runCatching {
                Log.d(TAG, "Enabling $rtcUiMode calls for identity $identity.")
                cache.identity = identity
                tokenProvider.getToken(identity)?.let { token ->
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

    override fun enableCalls(successListener: SuccessListener?, errorListener: ErrorListener?) {
        rtcUiMode = RtcUiMode.DEFAULT.withListeners(successListener, errorListener)
        pushIdDelegate.getPushRegistrationId()?.let { pushRegId ->
            enableCalls(
                identity = pushRegId,
                listenType = ListenType.PUSH,
                successListener = successListener,
                errorListener = errorListener
            )
        } ?: Log.d(TAG, "Could not obtain identity value(pushRegistrationId), waiting for broadcast.")
    }

    override fun enableInAppChatCalls(
        successListener: SuccessListener?,
        errorListener: ErrorListener?
    ) {
        rtcUiMode = RtcUiMode.IN_APP_CHAT.withListeners(successListener, errorListener)
        Log.d(TAG, "Waiting for broadcast with livechatRegistrationId.")
    }

    override fun disableCalls(successListener: SuccessListener?, errorListener: ErrorListener?) {
        runCatching {
            val identity = cache.identity
            require(identity.isNotEmpty()) { "Calls are not registered." }
            Log.d(TAG, "Disabling calls for identity $identity.")
            callsScope.launch {
                rtcInstance.disablePushNotification(tokenProvider.getToken(identity), context)
                cache.clear()
                successListener?.onSuccess()
                callsScope.coroutineContext.cancelChildren()
            }
        }.onFailure {
            errorListener?.onError(it)
        }
    }

    override fun setLanguage(locale: Locale) {
        Injector.locale = locale
    }

    override fun setInCallButtons(buttons: List<InCallButton>) {
        Injector.inCallButtons = listOf(InCallButton.HangUp, *buttons.toTypedArray())
    }

    override fun setTheme(theme: InfobipRtcUiTheme) {
        Injector.theme = theme
    }

    private fun registerPush(token: String, errorListener: ErrorListener?, successListener: SuccessListener?) {
        callsDelegate.enablePush(token, cache.configurationId) {
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
        with(Injector.cache) {
            rtcUiMode?.cleanListeners()
        }
    }
}


