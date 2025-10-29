/*
 * ThrottleFirstOnClickListener.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.utils

import android.view.View
import android.view.View.OnClickListener

class ThrottleFirstOnClickListener(
    private val interval: Long,
    private val listenerBlock: (View) -> Unit
) : OnClickListener {

    constructor(interval: Long, onClickListener: OnClickListener) : this(
        interval,
        { onClickListener.onClick(it) })

    private var lastClickTime = 0L

    override fun onClick(v: View) {
        val time = System.currentTimeMillis()
        if (time - lastClickTime >= interval) {
            lastClickTime = time
            listenerBlock(v)
        }
    }
}

fun View.setThrottleFirstOnClickListener(
    debounceInterval: Long = 1000L,
    listenerBlock: (View) -> Unit
) = setOnClickListener(ThrottleFirstOnClickListener(debounceInterval, listenerBlock))

fun View.setThrottleFirstOnClickListener(
    onClickListener: OnClickListener,
    debounceInterval: Long = 1000L
) = setOnClickListener(ThrottleFirstOnClickListener(debounceInterval, onClickListener))
