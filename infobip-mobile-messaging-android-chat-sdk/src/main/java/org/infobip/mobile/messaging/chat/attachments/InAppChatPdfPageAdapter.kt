/*
 * InAppChatPdfPageAdapter.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2026 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.attachments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.infobip.mobile.messaging.chat.R

/**
 * Shows pages of [renderer], rendering each page only once it is about to be displayed.
 *
 * @param renderer source of the rendered pages
 * @param scope scope the page rendering runs in, cancelled by the owner
 */
internal class InAppChatPdfPageAdapter(
    private val renderer: InAppChatPdfRenderer,
    private val scope: CoroutineScope,
) : RecyclerView.Adapter<InAppChatPdfPageAdapter.PageViewHolder>() {

    private val pageCount = renderer.pageCount

    override fun getItemCount(): Int = pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ib_item_chat_attach_pdf_page, parent, false)
        return PageViewHolder(view as InAppChatPdfPageView)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.render(position)
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    inner class PageViewHolder(private val pageView: InAppChatPdfPageView) : RecyclerView.ViewHolder(pageView) {

        private var renderJob: Job? = null

        fun render(position: Int) {
            recycle()
            // The page width is only known once the view is measured, doOnLayout runs the
            // rendering right away when it already is.
            pageView.doOnLayout {
                renderJob = scope.launch {
                    // The listener outlives a recycle, the holder can already show another page.
                    if (bindingAdapterPosition != position) return@launch
                    renderer.renderPage(position, it.width)?.let(pageView::setPage)
                }
            }
        }

        fun recycle() {
            renderJob?.cancel()
            renderJob = null
            // Clears the page along with its zoom, a recycled holder must not come back zoomed.
            pageView.setPage(null)
        }
    }
}
