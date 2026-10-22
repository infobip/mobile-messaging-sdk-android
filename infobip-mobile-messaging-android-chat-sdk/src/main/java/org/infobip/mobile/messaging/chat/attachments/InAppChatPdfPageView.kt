/*
 * InAppChatPdfPageView.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2026 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.attachments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * Shows a single rendered PDF page, zoomable by pinch and double tap and pannable while zoomed.
 *
 * The page is fitted to the view width when unzoomed, as a `fitCenter` [AppCompatImageView] would
 * show it. Zooming magnifies the rendered bitmap, it is not re-rendered at a higher resolution.
 */
internal class InAppChatPdfPageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val MIN_SCALE = 1f

        /**
         * Pages are rendered at a fixed resolution, past this the magnified bitmap is too soft
         * to be worth showing.
         */
        private const val MAX_SCALE = 3f
        private const val DOUBLE_TAP_SCALE = 2f
    }

    /** Fits the page into the view, the state the page is shown in when it is not zoomed. */
    private val baseMatrix = Matrix()

    /** Zoom and pan applied on top of [baseMatrix], in view coordinates. */
    private val zoomMatrix = Matrix()

    private val matrixValues = FloatArray(9)
    private val pageBounds = RectF()

    private val scale: Float
        get() = zoomMatrix.let {
            it.getValues(matrixValues)
            matrixValues[Matrix.MSCALE_X]
        }

    private val isZoomed: Boolean
        get() = scale > MIN_SCALE

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor.coerceIn(
                    MIN_SCALE / scale,
                    MAX_SCALE / scale
                )
                zoomMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                applyMatrix()
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isZoomed) {
                    zoomMatrix.reset()
                } else {
                    zoomMatrix.setScale(DOUBLE_TAP_SCALE, DOUBLE_TAP_SCALE, e.x, e.y)
                }
                applyMatrix()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!isZoomed) return false
                zoomMatrix.postTranslate(-distanceX, -distanceY)
                applyMatrix()
                return true
            }
        }
    )

    init {
        scaleType = ScaleType.MATRIX
    }

    /**
     * Shows [bitmap] as the page content, fitted to the view width and unzoomed.
     *
     * The view sizes itself to the bitmap's aspect ratio, [ScaleType.MATRIX] does not do it.
     *
     * @param bitmap rendered page, `null` clears the view
     */
    fun setPage(bitmap: Bitmap?) {
        setImageBitmap(bitmap)
        zoomMatrix.reset()
        val height = if (bitmap != null && bitmap.width > 0 && width > 0) {
            width * bitmap.height / bitmap.width
        } else {
            0
        }
        if (layoutParams != null && layoutParams.height != height) {
            layoutParams.height = height
            requestLayout()
        }
        updateBaseMatrix()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBaseMatrix()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        // While zoomed the page consumes the drag, the list must not scroll underneath it. Once
        // the page can not be panned any further the list takes the gesture back over.
        parent?.requestDisallowInterceptTouchEvent(isZoomed && canPanVertically())
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    /**
     * Fits the page into the view, keeping its aspect ratio, and reapplies the current zoom.
     */
    private fun updateBaseMatrix() {
        val drawable = drawable
        baseMatrix.reset()
        if (drawable != null && drawable.intrinsicWidth > 0 && width > 0) {
            val fitScale = width.toFloat() / drawable.intrinsicWidth
            baseMatrix.setScale(fitScale, fitScale)
        }
        applyMatrix()
    }

    private fun applyMatrix() {
        clampZoom()
        imageMatrix = Matrix(baseMatrix).apply { postConcat(zoomMatrix) }
    }

    /**
     * Keeps the page covering the view, so it can not be dragged away from the viewport edges.
     */
    private fun clampZoom() {
        zoomMatrix.getValues(matrixValues)
        val currentScale = matrixValues[Matrix.MSCALE_X]
        if (currentScale <= MIN_SCALE) {
            zoomMatrix.reset()
            return
        }
        val maxTranslateX = width * (currentScale - 1)
        val maxTranslateY = height * (currentScale - 1)
        matrixValues[Matrix.MTRANS_X] = matrixValues[Matrix.MTRANS_X].coerceIn(-maxTranslateX, 0f)
        matrixValues[Matrix.MTRANS_Y] = matrixValues[Matrix.MTRANS_Y].coerceIn(-maxTranslateY, 0f)
        zoomMatrix.setValues(matrixValues)
    }

    /**
     * `true` while the zoomed page has content left to pan to above or below the viewport.
     */
    private fun canPanVertically(): Boolean {
        pageBounds.set(0f, 0f, width.toFloat(), height.toFloat())
        zoomMatrix.mapRect(pageBounds)
        return pageBounds.top < 0f || pageBounds.bottom > height
    }
}
