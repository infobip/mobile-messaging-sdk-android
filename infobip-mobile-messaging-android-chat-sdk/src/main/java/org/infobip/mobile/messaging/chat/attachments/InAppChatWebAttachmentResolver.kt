/*
 * InAppChatWebAttachmentResolver.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.attachments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves [InAppChatWebAttachment] metadata which is not part of the attachment url.
 */
internal class InAppChatWebAttachmentResolver(
    private val openConnection: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection }
) {

    companion object {
        private const val TAG = "InAppChatWebAttachmentResolver"
        private const val TIMEOUT_MS = 10_000

        /**
         * Extension [android.webkit.URLUtil.guessFileName] falls back to when it can not
         * resolve a file name with a usable extension.
         */
        private const val UNRESOLVED_FILE_NAME_EXTENSION = ".bin"
    }

    /**
     * Resolves [InAppChatWebAttachment] for the attachment hosted at [url].
     *
     * Attachment urls are opaque, the file name is available only in the `Content-Disposition`
     * response header, so it is read using a HEAD request. Returns an attachment with
     * [fallbackFileName] when the header is missing or the request fails.
     *
     * @param url attachment url
     * @param fallbackFileName file name used when the real one can not be resolved
     * @return resolved [InAppChatWebAttachment]
     */
    suspend fun resolve(url: String, fallbackFileName: String): InAppChatWebAttachment =
        withContext(Dispatchers.IO) {
            val resolved = runCatching { readAttachment(url) }
                .onFailure { MobileMessagingLogger.w(TAG, "Could not resolve attachment file name.", it) }
                .getOrNull()

            // Without the header URLUtil.guessFileName() falls back to a meaningless ".bin" name.
            if (resolved == null || resolved.fileName.endsWith(UNRESOLVED_FILE_NAME_EXTENSION, ignoreCase = true)) {
                InAppChatWebAttachment(url, fallbackFileName)
            } else {
                resolved
            }
        }

    private fun readAttachment(url: String): InAppChatWebAttachment {
        val connection = openConnection(url).apply {
            requestMethod = "HEAD"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        return try {
            InAppChatWebAttachment(
                url = url,
                contentDisposition = connection.getHeaderField("Content-Disposition"),
                mimeType = connection.contentType
            )
        } finally {
            connection.disconnect()
        }
    }
}
