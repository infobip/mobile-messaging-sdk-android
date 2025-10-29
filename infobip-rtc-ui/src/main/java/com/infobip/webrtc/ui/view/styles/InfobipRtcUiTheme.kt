/*
 * InfobipRtcUiTheme.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.view.styles

data class InfobipRtcUiTheme @JvmOverloads constructor(
    val incomingCallScreenStyle: IncomingCallScreenStyle? = null,
    val inCallScreenStyle: InCallScreenStyle? = null,
    val colors: Colors? = null,
    val icons: Icons? = null,
)