package com.infobip.webrtc

import android.content.Context
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.ui.ErrorListener
import com.infobip.webrtc.ui.InfobipRtcUi
import com.infobip.webrtc.ui.InfobipRtcUiImpl
import com.infobip.webrtc.ui.SuccessListener
import com.infobip.webrtc.ui.delegate.*
import com.infobip.webrtc.ui.model.Colors
import com.infobip.webrtc.ui.model.Icons
import com.infobip.webrtc.ui.notifications.CallNotificationFactory
import com.infobip.webrtc.ui.notifications.CallNotificationFactoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.infobip.mobile.messaging.api.rtc.MobileApiRtc
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider

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
    var enableInAppCallsSuccess: SuccessListener? = null
    var enableInAppCallsError: ErrorListener? = null

    private val rtcService: MobileApiRtc by lazy { MobileApiResourceProvider().getMobileApiRtc(appContext) }
    private val tokenProvider: TokenProvider by lazy { TokenProviderImpl(rtcService) }
    private val pushIdDelegate: PushIdDelegate by lazy { PushIdDelegateImpl(appContext) }
    val appCodeDelegate: AppCodeDelegate by lazy { AppCodeDelegateImpl(appContext) }
    private var webrtcUi: InfobipRtcUi? = null

    fun getWebrtcUi(context: Context): InfobipRtcUi {
        if (!::appContext.isInitialized)
            appContext = context.applicationContext
        return webrtcUi ?: InfobipRtcUiImpl(
            appContext,
            tokenProvider,
            cache,
            callsDelegate,
            callsScope,
            pushIdDelegate,
            rtcInstance
        ).also { webrtcUi = it }
    }
}