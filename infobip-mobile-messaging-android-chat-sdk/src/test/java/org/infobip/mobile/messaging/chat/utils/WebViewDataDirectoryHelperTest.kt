/*
 * WebViewDataDirectoryHelperTest.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2026 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import android.system.Os
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class WebViewDataDirectoryHelperTest {

    private lateinit var tempDir: File
    private lateinit var context: Context

    companion object {
        private const val DEFAULT_DIR = "app_webview"
    }

    @Before
    fun setup() {
        tempDir = createTempDir("webview_test")
        val appInfo = ApplicationInfo().apply {
            dataDir = tempDir.absolutePath
        }
        context = mockk(relaxed = true)
        every { context.applicationInfo } returns appInfo

        mockkStatic(Os::class)
        mockkStatic(Process::class)
        every { Process.myPid() } returns 9999
    }

    @After
    fun tearDown() {
        unmockkStatic(Os::class)
        unmockkStatic(Process::class)
        tempDir.deleteRecursively()
    }

    //region cleanupStaleDataDirectoryIfNeeded() - default directory (app_webview)
    @Test
    fun `no app_webview directory - does nothing`() {
        WebViewDataDirectoryHelper.cleanupStaleDataDirectoryIfNeeded(context)

        verify(exactly = 0) { Os.readlink(any()) }
    }

    @Test
    fun `default dir exists but no SingletonLock - does nothing`() {
        val webViewDir = File(tempDir, DEFAULT_DIR)
        webViewDir.mkdirs()

        WebViewDataDirectoryHelper.cleanupStaleDataDirectoryIfNeeded(context)

        assertThat(webViewDir.exists()).isTrue()
    }

    @Test
    fun `default dir - SingletonLock with dead PID triggers cleanup`() {
        val webViewDir = File(tempDir, DEFAULT_DIR)
        webViewDir.mkdirs()
        File(webViewDir, "SingletonLock").createNewFile()
        File(webViewDir, "Cookies").createNewFile()

        every { Os.readlink(any()) } returns "localhost-12345"

        WebViewDataDirectoryHelper.cleanupStaleDataDirectoryIfNeeded(context)

        assertThat(webViewDir.exists()).isFalse()
    }

    @Test
    fun `default dir - SingletonLock with current PID does not trigger cleanup`() {
        val webViewDir = File(tempDir, DEFAULT_DIR)
        webViewDir.mkdirs()
        File(webViewDir, "SingletonLock").createNewFile()

        every { Os.readlink(any()) } returns "localhost-9999"

        WebViewDataDirectoryHelper.cleanupStaleDataDirectoryIfNeeded(context)

        assertThat(webViewDir.exists()).isTrue()
    }

    @Test
    fun `default dir - readlink throws exception - graceful fallback`() {
        val webViewDir = File(tempDir, DEFAULT_DIR)
        webViewDir.mkdirs()
        File(webViewDir, "SingletonLock").createNewFile()

        every { Os.readlink(any()) } throws RuntimeException("readlink failed")

        WebViewDataDirectoryHelper.cleanupStaleDataDirectoryIfNeeded(context)

        assertThat(webViewDir.exists()).isTrue()
    }

    @Test
    fun `default dir - unparseable symlink target - does nothing`() {
        val webViewDir = File(tempDir, DEFAULT_DIR)
        webViewDir.mkdirs()
        File(webViewDir, "SingletonLock").createNewFile()

        every { Os.readlink(any()) } returns "malformed_target"

        WebViewDataDirectoryHelper.cleanupStaleDataDirectoryIfNeeded(context)

        assertThat(webViewDir.exists()).isTrue()
    }

    @Test
    fun `default dir - non-numeric PID in symlink target - does nothing`() {
        val webViewDir = File(tempDir, DEFAULT_DIR)
        webViewDir.mkdirs()
        File(webViewDir, "SingletonLock").createNewFile()

        every { Os.readlink(any()) } returns "localhost-notanumber"

        WebViewDataDirectoryHelper.cleanupStaleDataDirectoryIfNeeded(context)

        assertThat(webViewDir.exists()).isTrue()
    }
    //endregion

    //region cleanupStaleDataDirectory() - reactive cleanup
    @Test
    fun `reactive cleanup deletes default directory`() {
        val defaultDir = File(tempDir, DEFAULT_DIR)
        defaultDir.mkdirs()
        File(defaultDir, "Cookies").createNewFile()

        val result = WebViewDataDirectoryHelper.cleanupStaleDataDirectory(context)

        assertThat(result).isTrue()
        assertThat(defaultDir.exists()).isFalse()
    }

    @Test
    fun `reactive cleanup succeeds when directory doesnt exist`() {
        val result = WebViewDataDirectoryHelper.cleanupStaleDataDirectory(context)

        assertThat(result).isTrue()
    }
    //endregion
}
