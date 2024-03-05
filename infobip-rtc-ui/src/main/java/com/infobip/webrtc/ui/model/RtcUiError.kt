package com.infobip.webrtc.ui.model

import com.infobip.webrtc.sdk.api.model.ErrorCode

data class RtcUiError(
    val id: Int,
    val name: String,
    val description: String?
) {

    constructor(errorCode: ErrorCode) : this(errorCode.id, errorCode.name, errorCode.description)

    companion object {
        @JvmField
        val MISSING_READ_PHONE_STATE_PERMISSION = RtcUiError(20_000, "MISSING_READ_PHONE_STATE_PERMISSION" , "Phone state permission not granted.")
        @JvmField
        val MISSING_POST_NOTIFICATIONS_PERMISSION = RtcUiError(20_001, "MISSING_POST_NOTIFICATIONS_PERMISSION" , "Post notifications permission not granted.")
        @JvmField
        val INCOMING_WEBRTC_CALL_WHILE_CELLULAR_CALL = RtcUiError(20_002, "INCOMING_WEBRTC_CALL_WHILE_CELLULAR_CALL" , "Incoming WebRTC call appeared while there is ringing/ongoing cellular call.")
        @JvmField
        val CELLULAR_CALL_ACCEPTED_WHILE_WEBRTC_CALL = RtcUiError(20_003, "CELLULAR_CALL_ACCEPTED_WHILE_WEBRTC_CALL" , "Cellular call accepted during ongoing WebRTC call.")
    }
}

