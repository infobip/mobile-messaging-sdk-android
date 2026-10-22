/*
 * InAppChatWebAttachmentResolverTest.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.attachments

import android.webkit.URLUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection

class InAppChatWebAttachmentResolverTest {

    private companion object {
        const val URL = "https://cloudfront.net/4B3130C9-DF4A-4E8B-8390-B094E7D953FA"
        const val FALLBACK_FILE_NAME = "attachment_1750000000000.jpg"
    }

    private val connection: HttpURLConnection = mockk(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(URLUtil::class)
        // Mimics URLUtil.guessFileName(), which falls back to a ".bin" name when it can not
        // resolve one with an extension from the given arguments.
        every { URLUtil.guessFileName(any(), any(), any()) } answers {
            val fileName = secondArg<String?>()
                ?.takeIf { it.contains("filename=") }
                ?.substringAfter("filename=")
                ?: "downloadfile"
            if (fileName.substringAfterLast('.', "").isEmpty()) "$fileName.bin" else fileName
        }
    }

    @After
    fun after() {
        unmockkStatic(URLUtil::class)
    }

    //region resolve()
    @Test
    fun `should resolve file name from Content-Disposition header`() {
        every { connection.getHeaderField("Content-Disposition") } returns
                "inline; filename=\"20250613_231952.jpg\"; filename*=UTF-8''20250613_231952.jpg"
        every { connection.contentType } returns "image/jpeg"

        val attachment = resolve()

        Assertions.assertThat(attachment.fileName)
            .describedAs("File name is parsed from the Content-Disposition header")
            .isEqualTo("20250613_231952.jpg")
        Assertions.assertThat(attachment.url).isEqualTo(URL)
    }

    @Test
    fun `should use fallback file name when Content-Disposition header is missing`() {
        every { connection.getHeaderField("Content-Disposition") } returns null
        every { connection.contentType } returns null

        Assertions.assertThat(resolve().fileName)
            .describedAs("Without the header there is nothing to resolve the file name from")
            .isEqualTo(FALLBACK_FILE_NAME)
    }

    @Test
    fun `should use fallback file name when resolved file name has no usable extension`() {
        every { connection.getHeaderField("Content-Disposition") } returns "inline"
        every { connection.contentType } returns null

        Assertions.assertThat(resolve().fileName)
            .describedAs("A '.bin' guess means the file name was not resolved")
            .isEqualTo(FALLBACK_FILE_NAME)
    }

    @Test
    fun `should use fallback file name when request fails`() {
        val resolver = InAppChatWebAttachmentResolver { throw IOException("No connection") }

        val attachment = runBlocking { resolver.resolve(URL, FALLBACK_FILE_NAME) }

        Assertions.assertThat(attachment.fileName)
            .describedAs("Failed request must not break the download")
            .isEqualTo(FALLBACK_FILE_NAME)
    }

    @Test
    fun `should send HEAD request and disconnect`() {
        every { connection.getHeaderField("Content-Disposition") } returns null
        every { connection.contentType } returns null

        resolve()

        verify { connection.requestMethod = "HEAD" }
        verify { connection.disconnect() }
    }
    //endregion

    private fun resolve(): InAppChatWebAttachment {
        val resolver = InAppChatWebAttachmentResolver { connection }
        return runBlocking { resolver.resolve(URL, FALLBACK_FILE_NAME) }
    }
}
