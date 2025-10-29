/*
 * CallAction.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.model

enum class CallAction {

    INCOMING_CALL_START,
    SILENT_INCOMING_CALL_START,
    INCOMING_CALL_ACCEPTED,
    CALL_DECLINE,
    CALL_HANGUP,
    CALL_RINGING,
    CALL_EARLY_MEDIA,
    CALL_ESTABLISHED,
    CALL_FINISHED,
    CALL_RECONNECTING,
    CALL_RECONNECTED;

    companion object {
        fun fromValue(name: String?): CallAction? {
            return entries.firstOrNull { it.name == name }
        }
    }

}