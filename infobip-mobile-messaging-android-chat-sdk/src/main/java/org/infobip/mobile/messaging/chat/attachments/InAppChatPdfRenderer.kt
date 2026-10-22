/*
 * InAppChatPdfRenderer.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.attachments

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

/**
 * Renders pages of a local PDF file into bitmaps.
 *
 * [PdfRenderer] allows only one open page at a time, so page rendering is serialized.
 * Instances hold native resources and must be closed, see [close].
 */
internal class InAppChatPdfRenderer private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : Closeable {

    companion object {
        /**
         * Width, in pixels, a rendered page bitmap is scaled down to when the requested
         * width is larger.
         *
         * [PdfRenderer.Page.render] requires an [Bitmap.Config.ARGB_8888] bitmap, so a page
         * costs `width * height * 4` bytes - roughly 6 MB at this width for an A4 page. Several
         * pages are held at once while scrolling, a larger bitmap risks exhausting the heap.
         */
        private const val MAX_PAGE_WIDTH_PX = 1080

        /**
         * Opens [file] for rendering.
         *
         * @param file local PDF file
         * @return renderer for [file]
         * @throws java.io.IOException when the file is not a valid PDF or is password protected
         */
        suspend fun open(file: File): InAppChatPdfRenderer = withContext(Dispatchers.IO) {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                InAppChatPdfRenderer(descriptor, PdfRenderer(descriptor))
            } catch (throwable: Throwable) {
                descriptor.close()
                throw throwable
            }
        }
    }

    private val lock = Any()
    private var closed = false

    val pageCount: Int
        get() = synchronized(lock) { if (closed) 0 else renderer.pageCount }

    /**
     * Renders page [index] into a bitmap [width] pixels wide, keeping the page's aspect ratio.
     *
     * @param index zero based page index
     * @param width requested bitmap width in pixels
     * @return rendered page, or `null` when this renderer is already closed
     */
    suspend fun renderPage(index: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (closed) return@withContext null
            renderer.openPage(index).use { page ->
                val targetWidth = width.coerceIn(1, MAX_PAGE_WIDTH_PX)
                val targetHeight = (targetWidth.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                val bitmap = createBitmap(targetWidth, targetHeight)
                // Pages are rendered with a transparent background, white keeps the text readable.
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
        }
    }
}
