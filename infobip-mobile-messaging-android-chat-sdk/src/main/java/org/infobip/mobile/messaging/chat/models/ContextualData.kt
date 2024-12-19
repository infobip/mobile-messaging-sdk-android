package org.infobip.mobile.messaging.chat.models

import org.infobip.mobile.messaging.chat.core.MultithreadStrategy

internal data class ContextualData(
    val data: String,
    val allMultiThreadStrategy: MultithreadStrategy
)