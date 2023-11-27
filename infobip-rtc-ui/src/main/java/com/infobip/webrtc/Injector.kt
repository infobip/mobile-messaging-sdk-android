package com.infobip.webrtc

import android.content.Context
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.ui.InfobipRtcUi
import com.infobip.webrtc.ui.InfobipRtcUiImpl
import com.infobip.webrtc.ui.delegate.AppCodeDelegate
import com.infobip.webrtc.ui.delegate.AppCodeDelegateImpl
import com.infobip.webrtc.ui.delegate.CallsDelegate
import com.infobip.webrtc.ui.delegate.CallsDelegateImpl
import com.infobip.webrtc.ui.delegate.NotificationPermissionDelegate
import com.infobip.webrtc.ui.delegate.NotificationPermissionDelegateImpl
import com.infobip.webrtc.ui.delegate.PushIdDelegate
import com.infobip.webrtc.ui.delegate.PushIdDelegateImpl
import com.infobip.webrtc.ui.delegate.Vibrator
import com.infobip.webrtc.ui.delegate.VibratorImpl
import com.infobip.webrtc.ui.model.InCallButton
import com.infobip.webrtc.ui.notifications.CallNotificationFactory
import com.infobip.webrtc.ui.notifications.CallNotificationFactoryImpl
import com.infobip.webrtc.ui.view.styles.Colors
import com.infobip.webrtc.ui.view.styles.Icons
import com.infobip.webrtc.ui.view.styles.IncomingCallMessageStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.infobip.mobile.messaging.api.rtc.MobileApiRtc
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider
import java.util.Locale

internal const val TAG = "InfobipRtcUi"

internal object Injector {
    private lateinit var appContext: Context
    private val callsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val rtcInstance = InfobipRTC.getInstance()
    val vibrator: Vibrator by lazy { VibratorImpl(appContext) }
    val cache: Cache = InMemoryCache()
    val notificationFactory: CallNotificationFactory by lazy { CallNotificationFactoryImpl(appContext) }
    val callsDelegate: CallsDelegate by lazy { CallsDelegateImpl(appContext, callsScope, rtcInstance) }
    var colors: Colors? = null
    var icons: Icons? = null
    var incomingCallMessageStyle: IncomingCallMessageStyle? = null
    var locale: Locale? = null
    var inCallButtons: List<InCallButton> = listOf(
        InCallButton.HangUp,
        InCallButton.Mute(),
        InCallButton.Video(),
        InCallButton.Speaker(),
        InCallButton.ScreenShare(),
        InCallButton.FlipCam(),
    )

    private val rtcService: MobileApiRtc by lazy { MobileApiResourceProvider().getMobileApiRtc(appContext) }
    private val tokenProvider: TokenProvider by lazy { TokenProviderImpl(rtcService) }
    private val pushIdDelegate: PushIdDelegate by lazy { PushIdDelegateImpl(appContext) }
    val appCodeDelegate: AppCodeDelegate by lazy { AppCodeDelegateImpl(appContext) }
    private var webrtcUi: InfobipRtcUi? = null
    val notificationPermissionDelegate: NotificationPermissionDelegate by lazy { NotificationPermissionDelegateImpl(appContext) }

    fun getWebrtcUi(context: Context): InfobipRtcUi {
        if (!::appContext.isInitialized)
            appContext = context.applicationContext
        return webrtcUi ?: runCatching {
            InfobipRtcUiImpl(
                appContext,
                tokenProvider,
                cache,
                callsDelegate,
                callsScope,
                pushIdDelegate,
                rtcInstance,
                notificationPermissionDelegate
            ).also { webrtcUi = it }
        }.getOrElse {
            //MobileApiRtc may fail to create if MM is not initialized
            throw IllegalStateException("Mobile messaging SDK not initialized. Please initialize it before calls.")
        }
    }
}