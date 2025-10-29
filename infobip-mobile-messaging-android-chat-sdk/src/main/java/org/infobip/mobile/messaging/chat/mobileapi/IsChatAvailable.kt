/*
 * IsChatAvailable.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.mobileapi

import org.infobip.mobile.messaging.MobileMessagingCore
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper

/**
 * Checks whether the in-app chat is ready to be shown to the user.
 * In-app chat is considered ready when the valid widget ID and push registration ID are available.
 */
internal object IsChatAvailable {

    @JvmStatic
    fun check(propertyHelper: PropertyHelper, mmCore: MobileMessagingCore): Boolean {
        val widgetId: String? = propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID)
        return check(widgetId, mmCore)
    }

    @JvmStatic
    fun check(widgetId: String?, mmCore: MobileMessagingCore): Boolean {
        return widgetId?.isNotBlank() == true && mmCore.pushRegistrationId?.isNotBlank() == true
    }

}