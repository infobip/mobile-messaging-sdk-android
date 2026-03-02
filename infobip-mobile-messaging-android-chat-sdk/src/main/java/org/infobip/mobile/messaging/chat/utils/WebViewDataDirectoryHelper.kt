/*
 * WebViewDataDirectoryHelper.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2026 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.utils

import android.content.Context
import android.os.Process
import android.system.Os
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import java.io.File

/**
 * Internal utility for managing WebView data directory lifecycle.
 *
 * This helper addresses a specific Android WebView issue where the data directory lock mechanism
 * can become stale after process termination, causing crashes on next app launch with error:
 * "Using WebView from more than one process at once with the same data directory is not supported"
 *
 * The WebView data directory (`/data/data/{package}/app_webview/`) contains lock state (exact
 * mechanism is internal to Chromium - could be file locks, shared memory, or database locks)
 * that persists on disk even after process death when WebView.destroy() was never called.
 * On next app launch, the new process detects the stale lock from the dead PID and throws RuntimeException.
 *
 * This utility provides safe cleanup of the entire WebView data directory when such errors occur,
 * clearing all stale state regardless of the specific lock mechanism used by WebView/Chromium.
 */
internal object WebViewDataDirectoryHelper {
    private const val TAG = "WebViewDataDirHelper"
    private const val SINGLETON_LOCK = "SingletonLock"

    /**
     * Default WebView data directory used by Android/Chromium when no suffix is set.
     */
    private const val WEBVIEW_DATA_DIR_DEFAULT = "app_webview"

    /**
     * Proactively detects and cleans up stale Chromium SingletonLock files.
     *
     * When a process is killed while WebView is active, Chromium's `SingletonLock` symlink in
     * the WebView data directory persists. On next app launch, any WebView creation (by any
     * library or the host app) will crash with "Using WebView from more than one process".
     *
     * This method checks the default (`app_webview/`) data directory. It reads the lock's
     * symlink target (format: `{hostname}-{pid}`), checks if that PID is still alive, and
     * deletes the data directory only when the owning process is confirmed dead.
     *
     * Should be called **early** during SDK initialization — before any WebView is created.
     * All operations are wrapped in try-catch so this method never throws.
     *
     * @param context Application context
     */
    fun cleanupStaleDataDirectoryIfNeeded(context: Context) {
        try {
            val webViewDir = File(context.applicationInfo.dataDir, WEBVIEW_DATA_DIR_DEFAULT)
            if (!webViewDir.exists()) return

            val lockFile = File(webViewDir, SINGLETON_LOCK)
            if (!lockFile.exists()) return

            val target = Os.readlink(lockFile.absolutePath) // e.g. "hostname-12345"
            val pid = extractPid(target) ?: return

            if (pid == Process.myPid()) return

            if (!File("/proc/$pid").exists()) {
                MobileMessagingLogger.w(TAG, "Stale WebView lock detected in $WEBVIEW_DATA_DIR_DEFAULT (dead PID $pid), cleaning up")
                deleteDirectory(webViewDir)
            }
        } catch (e: Exception) {
            MobileMessagingLogger.d(TAG, "Stale lock check skipped: ${e.message}")
        }
    }

    /**
     * Extracts the PID from a Chromium SingletonLock symlink target.
     * Expected format: `{hostname}-{pid}` (e.g., "localhost-12345").
     */
    private fun extractPid(target: String): Int? {
        val lastDash = target.lastIndexOf('-')
        if (lastDash < 0 || lastDash == target.length - 1) return null
        return target.substring(lastDash + 1).toIntOrNull()
    }

    /**
     * Reactively cleans up WebView data directory after a multi-process lock crash.
     *
     * Deletes the default WebView data directory. This method should be called after WebView
     * initialization fails with a lock error, as it deletes WebView cache, cookies, and
     * session data.
     *
     * @param context Application context
     * @return true if cleanup was successful or directory didn't exist, false on error
     */
    fun cleanupStaleDataDirectory(context: Context): Boolean {
        val webViewDir = File(context.applicationInfo.dataDir, WEBVIEW_DATA_DIR_DEFAULT)
        return if (webViewDir.exists()) deleteDirectory(webViewDir) else true
    }

    private fun deleteDirectory(dir: File): Boolean {
        return try {
            MobileMessagingLogger.w(TAG, "Cleaning up WebView data directory: ${dir.absolutePath}")
            val deleted = dir.deleteRecursively()
            if (deleted) {
                MobileMessagingLogger.d(TAG, "WebView data directory cleaned successfully: ${dir.absolutePath}")
            } else {
                MobileMessagingLogger.w(TAG, "WebView data directory cleanup returned false: ${dir.absolutePath}")
            }
            deleted
        } catch (e: Exception) {
            MobileMessagingLogger.e(TAG, "Failed to clean WebView data directory ${dir.absolutePath}: ${e.message}", e)
            false
        }
    }
}
