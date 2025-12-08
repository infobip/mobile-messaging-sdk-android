/*
 * Injector.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.core

import android.content.Context
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.ui.InfobipRtcUi
import com.infobip.webrtc.ui.internal.delegate.AppCodeDelegate
import com.infobip.webrtc.ui.internal.delegate.AppCodeDelegateImpl
import com.infobip.webrtc.ui.internal.delegate.CallsDelegate
import com.infobip.webrtc.ui.internal.delegate.CallsDelegateImpl
import com.infobip.webrtc.ui.internal.delegate.NotificationPermissionDelegate
import com.infobip.webrtc.ui.internal.delegate.NotificationPermissionDelegateImpl
import com.infobip.webrtc.ui.internal.delegate.PushIdDelegate
import com.infobip.webrtc.ui.internal.delegate.PushIdDelegateImpl
import com.infobip.webrtc.ui.internal.notification.CallNotificationFactory
import com.infobip.webrtc.ui.internal.notification.CallNotificationFactoryImpl
import com.infobip.webrtc.ui.logging.RtcUiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.infobip.mobile.messaging.api.rtc.MobileApiRtc
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider

internal object Injector {

    private lateinit var appContext: Context
    private var webrtcUi: InfobipRtcUi? = null

    private val callsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val rtcInstance = InfobipRTC.getInstance()
    private val rtcService: MobileApiRtc by lazy { MobileApiResourceProvider().getMobileApiRtc(appContext) }
    private val tokenProvider: TokenProvider by lazy { TokenProviderImpl(rtcService) }
    private val pushIdDelegate: PushIdDelegate by lazy { PushIdDelegateImpl(appContext) }
    private val notificationPermissionDelegate: NotificationPermissionDelegate by lazy { NotificationPermissionDelegateImpl(appContext) }

    val cache: Cache = InMemoryCache()
    val notificationFactory: CallNotificationFactory by lazy { CallNotificationFactoryImpl(appContext) }
    val callsDelegate: CallsDelegate by lazy { CallsDelegateImpl(appContext, rtcInstance) }
    val appCodeDelegate: AppCodeDelegate by lazy { AppCodeDelegateImpl(appContext) }

    fun getWebrtcUi(context: Context): InfobipRtcUi {
        if (!Injector::appContext.isInitialized) {
            appContext = context.applicationContext
            RtcUiLogger.init(appContext)
        }
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