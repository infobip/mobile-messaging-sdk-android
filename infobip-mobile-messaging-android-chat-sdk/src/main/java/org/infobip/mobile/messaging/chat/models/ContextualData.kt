/*
 * ContextualData.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.models

import org.infobip.mobile.messaging.chat.core.MultithreadStrategy

internal data class ContextualData(
    val data: String,
    val allMultiThreadStrategy: MultithreadStrategy
)