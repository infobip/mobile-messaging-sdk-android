package com.infobip.webrtc.ui.internal.core

import android.content.Context
import android.util.Log
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.sdk.api.model.push.Status
import com.infobip.webrtc.ui.ErrorListener
import com.infobip.webrtc.ui.InfobipRtcUi
import com.infobip.webrtc.ui.RtcUiCallErrorMapper
import com.infobip.webrtc.ui.SuccessListener
import com.infobip.webrtc.ui.internal.delegate.CallsDelegate
import com.infobip.webrtc.ui.internal.delegate.NotificationPermissionDelegate
import com.infobip.webrtc.ui.internal.delegate.PushIdDelegate
import com.infobip.webrtc.ui.internal.model.RtcUiMode
import com.infobip.webrtc.ui.model.InCallButton
import com.infobip.webrtc.ui.model.ListenType
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

    override fun enableCalls(
        identity: String,
        listenType: ListenType,
        successListener: SuccessListener?,
        errorListener: ErrorListener?
    ) {
        //it can be called also from broadcast receivers when we already have mode
        if (rtcUiMode == null)
            rtcUiMode = RtcUiMode.CUSTOM.withListeners(successListener, errorListener)

        val onError: (Throwable) -> Unit = {
            runOnUiThread { errorListener?.onError(it) }
            cleanStoredCallbacks()
        }

        val onSuccess: (Unit) -> Unit = {
            cache.identity = identity
            runOnUiThread { successListener?.onSuccess() }
            cleanStoredCallbacks()
        }

        callsScope.launch {
            runCatching {
                Log.d(TAG, "Enabling $rtcUiMode calls for identity $identity.")
                tokenProvider.getToken(identity)?.let { token ->
                    if (listenType == ListenType.PUSH) {
                        if (!notificationPermissionDelegate.hasPermission()) {
                            withContext(Dispatchers.Main) {
                                notificationPermissionDelegate.request()
                            }
                        }
                        registerPush(token, onError, onSuccess)
                    } else {
                        registerActiveConnection(token, onError, onSuccess)
                    }
                } ?: onError(IllegalStateException("Missing WebRTC token."))
            }.onFailure(onError)
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
        } ?: Log.d(
            TAG,
            "Could not obtain identity value(pushRegistrationId), waiting for broadcast."
        )
    }

    override fun enableInAppChatCalls(
        successListener: SuccessListener?,
        errorListener: ErrorListener?
    ) {
        rtcUiMode = RtcUiMode.IN_APP_CHAT.withListeners(successListener, errorListener)
        cache.livechatRegistrationId?.let {
            enableCalls(
                identity = it,
                listenType = ListenType.PUSH,
                successListener = successListener,
                errorListener = errorListener
            )
        } ?: Log.d(TAG, "Waiting for broadcast with livechatRegistrationId.")
    }

    override fun disableCalls(successListener: SuccessListener?, errorListener: ErrorListener?) {
        runCatching {
            val identity = cache.identity
            require(identity.isNotEmpty()) { "Calls are not registered." }
            Log.d(TAG, "Disabling calls for identity $identity.")
            callsScope.launch {
                val webRtcToken = tokenProvider.getToken(identity)
                require(webRtcToken?.isNotBlank() == true) { "Missing WebRTC token." }
                rtcInstance.disablePushNotification(webRtcToken.orEmpty(), context)
                cache.clear()
                runOnUiThread { successListener?.onSuccess() }
                callsScope.coroutineContext.cancelChildren()
            }
        }.onFailure {
            runOnUiThread { errorListener?.onError(it) }
        }
    }

    override fun setLanguage(locale: Locale) {
        cache.locale = locale
    }

    override fun setInCallButtons(buttons: List<InCallButton>) {
        cache.inCallButtons = listOf(InCallButton.HangUp, *buttons.toTypedArray())
    }

    override fun setTheme(theme: InfobipRtcUiTheme) {
        cache.theme = theme
    }

    override fun setErrorMapper(errorMapper: RtcUiCallErrorMapper) {
        cache.callErrorMapper = errorMapper
    }

    private fun registerPush(
        token: String,
        onError: (Throwable) -> Unit = {},
        onSuccess: (Unit) -> Unit = {},
    ) {
        callsDelegate.enablePush(token, cache.configurationId) {
            Log.d(TAG, "Registration for calls push result: ${it.status}, ${it.description}")
            if (it.status != Status.SUCCESS) {
                onError(IllegalStateException("Registration for calls push failed. Reason: ${it.description}"))
            } else {
                onSuccess(Unit)
            }
        }
    }

    private fun registerActiveConnection(
        token: String,
        onError: (Throwable) -> Unit = {},
        onSuccess: (Unit) -> Unit = {},
    ) {
        runCatching { callsDelegate.registerActiveConnection(token) }
            .fold(onSuccess, onError)
    }

    private fun runOnUiThread(action: () -> Unit) {
        callsScope.launch(Dispatchers.Main) {
            action()
        }
    }

    private fun cleanStoredCallbacks() {
        cache.rtcUiMode?.cleanListeners()
    }
}