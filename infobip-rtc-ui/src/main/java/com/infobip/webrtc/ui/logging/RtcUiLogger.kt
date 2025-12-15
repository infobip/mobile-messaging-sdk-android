/*
 * RtcUiLogger.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.logging

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import com.infobip.webrtc.ui.logging.RtcUiLogger.enforce

/**
 * Centralized logging for InfobipRtcUi module.
 * Thread-safe singleton for module-internal use.
 *
 * Logging is automatically enabled in debuggable builds.
 * Use [enforce] to enable logging in release builds for troubleshooting.
 */
@SuppressLint("StaticFieldLeak")
object RtcUiLogger {

    private const val DEFAULT_TAG = "InfobipRtcUi"

    @Volatile
    private var context: Context? = null

    @Volatile
    private var writer: RtcUiWriter = RtcUiLogcatWriter()

    @Volatile
    private var isEnforced: Boolean = false

    /**
     * Initialize logger with application context.
     * Must be called early in module initialization.
     *
     * @param context Application context
     */
    fun init(context: Context) {
        this.context = context.applicationContext
    }

    /**
     * Force logging to be enabled regardless of debuggable flag.
     * Useful for troubleshooting production issues.
     */
    fun enforce() {
        isEnforced = true
    }

    /**
     * Set custom writer implementation.
     * Can be used to redirect logs to custom destinations (file, remote server, etc.).
     *
     * @param logWriter Custom writer implementation
     */
    fun setWriter(logWriter: RtcUiWriter) {
        writer = logWriter
    }

    /**
     * Reset logger to default state.
     * Clears custom writer and enforcement flag.
     */
    fun reset() {
        isEnforced = false
        writer = RtcUiLogcatWriter()
    }

    /**
     * Check if logging is currently enabled.
     * Returns true if app is debuggable OR logging is enforced.
     *
     * @return true if logging is enabled
     */
    fun isLoggingEnabled(): Boolean {
        val isDebuggable = context?.let { ctx -> (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 } ?: false
        return isEnforced || isDebuggable
    }

    /**
     * Log a verbose message.
     *
     * @param message The message to log
     * @param tag Optional log tag (defaults to "InfobipRtcUi")
     * @param throwable Optional exception to log
     */
    @JvmStatic
    @JvmOverloads
    fun v(message: String, tag: String? = null, throwable: Throwable? = null) {
        log(RtcUiLogLevel.VERBOSE, tag ?: DEFAULT_TAG, message, throwable)
    }

    /**
     * Log a debug message.
     *
     * @param message The message to log
     * @param tag Optional log tag (defaults to "InfobipRtcUi")
     * @param throwable Optional exception to log
     */
    @JvmStatic
    @JvmOverloads
    fun d(message: String, tag: String? = null, throwable: Throwable? = null) {
        log(RtcUiLogLevel.DEBUG, tag ?: DEFAULT_TAG, message, throwable)
    }

    /**
     * Log an info message.
     *
     * @param message The message to log
     * @param tag Optional log tag (defaults to "InfobipRtcUi")
     * @param throwable Optional exception to log
     */
    @JvmStatic
    @JvmOverloads
    fun i(message: String, tag: String? = null, throwable: Throwable? = null) {
        log(RtcUiLogLevel.INFO, tag ?: DEFAULT_TAG, message, throwable)
    }

    /**
     * Log a warning message.
     *
     * @param message The message to log
     * @param tag Optional log tag (defaults to "InfobipRtcUi")
     * @param throwable Optional exception to log
     */
    @JvmStatic
    @JvmOverloads
    fun w(message: String, tag: String? = null, throwable: Throwable? = null) {
        log(RtcUiLogLevel.WARN, tag ?: DEFAULT_TAG, message, throwable)
    }

    /**
     * Log an error message.
     *
     * @param message The message to log
     * @param tag Optional log tag (defaults to "InfobipRtcUi")
     * @param throwable Optional exception to log
     */
    @JvmStatic
    @JvmOverloads
    fun e(message: String, tag: String? = null, throwable: Throwable? = null) {
        log(RtcUiLogLevel.ERROR, tag ?: DEFAULT_TAG, message, throwable)
    }

    private fun log(level: RtcUiLogLevel, tag: String, message: String, throwable: Throwable?) {
        if (isLoggingEnabled()) {
            writer.write(level, tag, message, throwable)
        }
    }
}
