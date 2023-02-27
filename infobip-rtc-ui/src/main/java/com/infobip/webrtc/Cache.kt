package com.infobip.webrtc

import android.app.Activity
import com.infobip.webrtc.ui.CallActivity

internal interface Cache {
    var applicationId: String
    var identity: String
    var inAppCallsEnabled: Boolean
    var activityClass: Class<out Activity>

    fun clear()
}

internal class InMemoryCache : Cache {
    override var applicationId: String = ""
    override var identity: String = ""
    override var inAppCallsEnabled: Boolean = false
    override var activityClass: Class<out Activity> = CallActivity::class.java

    override fun clear() {
        identity = ""
        inAppCallsEnabled = false
        activityClass = CallActivity::class.java
    }
}