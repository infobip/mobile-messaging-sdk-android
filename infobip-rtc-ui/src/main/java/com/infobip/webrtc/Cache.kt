package com.infobip.webrtc

import android.app.Activity
import com.infobip.webrtc.ui.CallActivity
import com.infobip.webrtc.ui.model.RtcUiMode

internal interface Cache {
    var configurationId: String
    var identity: String
    var activityClass: Class<out Activity>
    var autoDeclineOnMissingNotificationPermission: Boolean
    var rtcUiMode: RtcUiMode?

    fun clear()
}

internal class InMemoryCache : Cache {
    override var configurationId: String = ""
    override var identity: String = ""
    override var activityClass: Class<out Activity> = CallActivity::class.java
    override var autoDeclineOnMissingNotificationPermission: Boolean = true
    override var rtcUiMode: RtcUiMode? = null

    override fun clear() {
        identity = ""
        rtcUiMode = null
    }
}