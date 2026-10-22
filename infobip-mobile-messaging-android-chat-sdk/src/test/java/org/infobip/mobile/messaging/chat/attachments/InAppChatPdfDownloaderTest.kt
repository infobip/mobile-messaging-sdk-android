/*
 * InAppChatPdfDownloaderTest.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.attachments

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

class InAppChatPdfDownloaderTest {

    private companion object {
        const val URL = "https://cloudfront.net/4B3130C9-DF4A-4E8B-8390-B094E7D953FA"
        val CONTENT = "%PDF-1.4 content".toByteArray()
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val connection: HttpURLConnection = mockk(relaxed = true)

    //region download()
    @Test
    fun `should write response body to destination`() {
        givenResponse(code = 200, body = CONTENT)
        val destination = File(temporaryFolder.root, "nested/attachment.pdf")

        val file = runBlocking { downloader().download(URL, destination) }

        Assertions.assertThat(file.readBytes())
            .describedAs("Downloaded content is written to the destination")
            .isEqualTo(CONTENT)
    }

    @Test
    fun `should create missing parent directories`() {
        givenResponse(code = 200, body = CONTENT)
        val destination = File(temporaryFolder.root, "a/b/c/attachment.pdf")

        runBlocking { downloader().download(URL, destination) }

        Assertions.assertThat(destination)
            .describedAs("Cache subdirectory is created on demand")
            .exists()
    }

    @Test
    fun `should fail on error response`() {
        givenResponse(code = 403, body = ByteArray(0))
        val destination = File(temporaryFolder.root, "attachment.pdf")

        Assertions.assertThatThrownBy { runBlocking { downloader().download(URL, destination) } }
            .describedAs("An expired or forbidden url must not be reported as a successful download")
            .isInstanceOf(IOException::class.java)
    }

    @Test
    fun `should not leave a partially written file behind`() {
        every { connection.responseCode } returns 200
        every { connection.inputStream } returns FailingInputStream(CONTENT)
        val destination = File(temporaryFolder.root, "attachment.pdf")

        runCatching { runBlocking { downloader().download(URL, destination) } }

        Assertions.assertThat(destination)
            .describedAs("A partially downloaded file is not renderable and must be removed")
            .doesNotExist()
    }

    @Test
    fun `should disconnect when download fails`() {
        givenResponse(code = 500, body = ByteArray(0))

        runCatching { runBlocking { downloader().download(URL, File(temporaryFolder.root, "a.pdf")) } }

        verify { connection.disconnect() }
    }
    //endregion

    private fun givenResponse(code: Int, body: ByteArray) {
        every { connection.responseCode } returns code
        every { connection.inputStream } returns ByteArrayInputStream(body)
    }

    private fun downloader() = InAppChatPdfDownloader { connection }

    /**
     * Stream which fails midway, as a connection dropped during the download does.
     */
    private class FailingInputStream(content: ByteArray) : InputStream() {

        private val delegate = ByteArrayInputStream(content)
        private var read = 0

        override fun read(): Int {
            if (read++ >= 4) throw IOException("Connection lost")
            return delegate.read()
        }
    }
}
