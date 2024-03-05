package com.infobip.webrtc.ui.internal.core

import android.content.Context
import com.infobip.webrtc.sdk.api.model.ErrorCode
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.RtcUiCallErrorMapper
import com.infobip.webrtc.ui.model.RtcUiError

internal object RtcUiCallErrorMapperFactory {
    fun create(context: Context): RtcUiCallErrorMapper {
        return Injector.cache.callErrorMapper ?: InternalRtcUiCallErrorMapper(context)
    }
}

private class InternalRtcUiCallErrorMapper(
    private val context: Context
) : RtcUiCallErrorMapper {

    override fun getMessageForError(error: RtcUiError): String? {
        return when (error.name) {
            RtcUiError.CELLULAR_CALL_ACCEPTED_WHILE_WEBRTC_CALL.name -> context.getString(R.string.mm_ongoing_cellular_call_call_finished)
            RtcUiError.INCOMING_WEBRTC_CALL_WHILE_CELLULAR_CALL.name -> context.getString(R.string.mm_ongoing_cellular_call_declining_call)
            RtcUiError.MISSING_READ_PHONE_STATE_PERMISSION.name -> context.getString(R.string.mm_read_phone_state_permission_required_declining_call)
            RtcUiError.MISSING_POST_NOTIFICATIONS_PERMISSION.name -> context.getString(R.string.mm_notification_permission_required_declining_call)
            ErrorCode.UNKNOWN.name -> context.getString(R.string.mm_unknown_error)
            ErrorCode.NORMAL_HANGUP.name -> null
            else -> error.description ?: error.name
        }
    }

}