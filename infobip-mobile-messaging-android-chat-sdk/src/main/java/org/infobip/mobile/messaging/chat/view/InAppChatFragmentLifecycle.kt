/*
 * InAppChatFragmentLifecycle.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Copies lifecycle from provided [lifecycleOwner] with option to ignore event based on provided condition [ignoreLifecycleOwnerEventsWhen].
 * Allows to manually mark lifecycle state using [setState].
 * [isEnabled] disables new lifecycle state propagation.
 */
internal interface InAppChatFragmentLifecycleRegistry {
    val lifecycle: Lifecycle
    var isEnabled: Boolean
    fun setState(state: Lifecycle.State)
}

internal class InAppChatFragmentLifecycleRegistryImpl(
    private val lifecycleOwner: LifecycleOwner,
    private val ignoreLifecycleOwnerEventsWhen: () -> Boolean,
) : LifecycleRegistry(lifecycleOwner), InAppChatFragmentLifecycleRegistry {
    override var isEnabled: Boolean = true

    private val observer = object : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val state = event.targetState
            if (!ignoreLifecycleOwnerEventsWhen() && isEnabled) {
                if (this@InAppChatFragmentLifecycleRegistryImpl.currentState != state)
                    this@InAppChatFragmentLifecycleRegistryImpl.currentState = state
            }
            if (state == State.DESTROYED)
                lifecycleOwner.lifecycle.removeObserver(this)
        }

    }

    init {
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    override val lifecycle: Lifecycle
        get() = this

    override fun setState(state: State) {
        this.currentState = state
    }

}