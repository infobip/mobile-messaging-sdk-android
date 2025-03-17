package org.infobip.mobile.messaging.chat.core;

public enum MultithreadStrategy {
    /**
     * Send metadata only to the currently opened active thread.
     */
    ACTIVE,
    /**
     * Send metadata to all existing threads.
     */
    ALL,
    /**
     * Send metadata to all existing threads. If new thread is created, it will send metadata to it as well.
     */
    ALL_PLUS_NEW
}
