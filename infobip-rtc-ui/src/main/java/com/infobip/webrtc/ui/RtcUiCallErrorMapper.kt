package com.infobip.webrtc.ui

import com.infobip.webrtc.ui.model.RtcUiError

fun interface RtcUiCallErrorMapper {

    /**
     * The function converts [RtcUiError] to a message that will be displayed to the user.
     * You can find all possible errors defined by InfobipRtcUi library in [RtcUiError.Companion] class plus there
     * are general WebRTC error codes defined in Infobip [documentation](https://www.infobip.com/docs/essentials/response-status-and-error-codes#webrtc-error-codes).
     *
     * @param error InfobipRtcUi error
     * @return message to be show to the user, if the message is null or blank, it is not shown to the user
     */
    fun getMessageForError(error: RtcUiError): String?

}