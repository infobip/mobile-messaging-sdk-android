/*
 * RtcUiWriter.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.logging

/**
 * Interface for custom log output destinations.
 * Default implementation writes to Android Logcat via [RtcUiLogcatWriter].
 *
 * Implement this interface to redirect logs to custom destinations
 * (e.g., file, remote server, analytics platform).
 */
interface RtcUiWriter {
    /**
     * Writes a log entry.
     *
     * @param level The severity level of the log entry
     * @param tag The log tag for filtering
     * @param message The log message
     * @param throwable Optional exception to log
     */
    fun write(level: RtcUiLogLevel, tag: String, message: String, throwable: Throwable? = null)
}
