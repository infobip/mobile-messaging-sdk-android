/*
 * Cache.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.core

import android.app.Activity
import com.infobip.webrtc.ui.RtcUiCallErrorMapper
import com.infobip.webrtc.ui.internal.model.RtcUiMode
import com.infobip.webrtc.ui.internal.ui.CallActivity
import com.infobip.webrtc.ui.model.InCallButton
import com.infobip.webrtc.ui.view.styles.Colors
import com.infobip.webrtc.ui.view.styles.Icons
import com.infobip.webrtc.ui.view.styles.InCallScreenStyle
import com.infobip.webrtc.ui.view.styles.IncomingCallScreenStyle
import com.infobip.webrtc.ui.view.styles.InfobipRtcUiTheme
import java.util.Locale

//region Cache
internal interface Cache : CallRegistrationLifetimeCache, SdkLifetimeCache {
    override fun clear()
}

internal class InMemoryCache(
    private val callsCache: CallRegistrationLifetimeCache = CallRegistrationLifetimeCacheImpl(),
    sdkCache: SdkLifetimeCache = SdkLifetimeCacheImpl()
) : Cache, CallRegistrationLifetimeCache by callsCache, SdkLifetimeCache by sdkCache {

    override fun clear() {
        callsCache.clear()
    }

}
//endregion

//region CallsRegistrationLifetimeCache
/**
 * Contains everything what has to be cached only for one calls registration ([InfobipRtcUi.enableCalls] - [InfobipRtcUi.disableCalls]).
 * Cache is cleared when [InfobipRtcUi.disableCalls] is called.
 */
internal interface CallRegistrationLifetimeCache {
    var identity: String
    var rtcUiMode: RtcUiMode?
    fun clear()
}

internal class CallRegistrationLifetimeCacheImpl : CallRegistrationLifetimeCache {

    override var identity: String = ""
    override var rtcUiMode: RtcUiMode? = null

    override fun clear() {
        identity = ""
        rtcUiMode = null
    }
}
//endregion

//region SdkLifetimeCache
/**
 * Contains everything what has to be cached for whole SDK lifetime.
 * Cache is cleared when SDK is removed from memory - app is killed.
 */
internal interface SdkLifetimeCache {
    var configurationId: String
    var activityClass: Class<out Activity>
    var autoDeclineOnMissingNotificationPermission: Boolean
    var autoDeclineOnMissingReadPhoneStatePermission: Boolean
    var autoDeclineWhenOngoingCellularCall: Boolean
    var autoFinishWhenIncomingCellularCallAccepted: Boolean
    var locale: Locale?
    var theme: InfobipRtcUiTheme?
    val colors: Colors?
    val icons: Icons?
    val incomingCallScreenStyle: IncomingCallScreenStyle?
    val inCallScreenStyle: InCallScreenStyle?
    var inCallButtons: List<InCallButton>
    var callErrorMapper: RtcUiCallErrorMapper?

    /**
     * Last reported livechat registration id. It can be used to re-enable in-app chat calls multiple times.
     */
    var livechatRegistrationId: String?
}

internal class SdkLifetimeCacheImpl : SdkLifetimeCache {

    override var configurationId: String = ""
    override var activityClass: Class<out Activity> = CallActivity::class.java
    override var autoDeclineOnMissingNotificationPermission: Boolean = true
    override var autoDeclineOnMissingReadPhoneStatePermission: Boolean = false
    override var autoDeclineWhenOngoingCellularCall: Boolean = true
    override var autoFinishWhenIncomingCellularCallAccepted: Boolean = true
    override var locale: Locale? = null
    override var theme: InfobipRtcUiTheme? = null
    override val colors: Colors?
        get() = theme?.colors
    override val icons: Icons?
        get() = theme?.icons
    override val incomingCallScreenStyle: IncomingCallScreenStyle?
        get() = theme?.incomingCallScreenStyle
    override val inCallScreenStyle: InCallScreenStyle?
        get() = theme?.inCallScreenStyle

    override var inCallButtons: List<InCallButton> = emptyList()
        get() = field.takeIf { it.isNotEmpty() } ?: listOf(
            InCallButton.HangUp,
            InCallButton.Mute(),
            InCallButton.Video(),
            InCallButton.Speaker(),
            InCallButton.ScreenShare(),
            InCallButton.FlipCam(),
        )
    override var callErrorMapper: RtcUiCallErrorMapper? = null
    override var livechatRegistrationId: String? = null
}
//endregion


