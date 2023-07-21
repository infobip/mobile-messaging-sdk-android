package org.infobip.mobile.messaging.chat.view

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

internal interface InAppChatFragmentLifecycleRegistry {
    val lifecycle: Lifecycle
    fun setState(state: Lifecycle.State)
}

/**
 * Copies lifecycle from provided [lifecycleOwner] with option to ignore event based on provided condition [ignoreLifecycleOwnerEventsWhen].
 * Allows to manually mark lifecycle state using [setState].
 */
internal class InAppChatFragmentLifecycleRegistryImpl(
    private val lifecycleOwner: LifecycleOwner,
    private val ignoreLifecycleOwnerEventsWhen: () -> Boolean,
) : LifecycleRegistry(lifecycleOwner), InAppChatFragmentLifecycleRegistry {

    private val observer = object : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val state = event.targetState
            if (!ignoreLifecycleOwnerEventsWhen()) {
                if (this@InAppChatFragmentLifecycleRegistryImpl.currentState != state)
                    this@InAppChatFragmentLifecycleRegistryImpl.currentState = state
            }
            if (state == Lifecycle.State.DESTROYED)
                lifecycleOwner.lifecycle.removeObserver(this)
        }

    }

    init {
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    override val lifecycle: Lifecycle
        get() = this

    override fun setState(state: Lifecycle.State) {
        this.currentState = state
    }

}