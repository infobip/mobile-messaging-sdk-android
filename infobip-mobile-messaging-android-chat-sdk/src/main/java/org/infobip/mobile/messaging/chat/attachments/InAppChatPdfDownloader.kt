/*
 * InAppChatPdfDownloader.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.attachments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads an attachment into the application's cache so it can be rendered locally.
 */
internal class InAppChatPdfDownloader(
    private val openConnection: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection }
) {

    companion object {
        private const val TIMEOUT_MS = 30_000
    }

    /**
     * Downloads the attachment hosted at [url] into [destination].
     *
     * @param url attachment url
     * @param destination file the attachment is written to, overwritten when it already exists
     * @return [destination]
     * @throws IOException when the attachment can not be downloaded
     */
    suspend fun download(url: String, destination: File): File = withContext(Dispatchers.IO) {
        val connection = openConnection(url).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Attachment download failed with response code ${connection.responseCode}.")
            }
            destination.parentFile?.mkdirs()
            try {
                connection.inputStream.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (throwable: Throwable) {
                // A partially written file is not renderable, it must not be left behind.
                destination.delete()
                throw throwable
            }
            destination
        } finally {
            connection.disconnect()
        }
    }
}
