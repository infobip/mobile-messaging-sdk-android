/*
 * InAppChatSnackbar.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2026 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view

import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.snackbar.Snackbar
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

object InAppChatSnackbar {

    /**
     * Shows a snackbar styled for connection errors using network error style properties.
     * The message text is determined from the style's networkConnectionErrorTextRes or networkConnectionErrorText.
     *
     * @param view The view to find a parent from for anchoring the snackbar
     * @param style The InAppChatStyle containing network error styling and message text
     * @param duration Snackbar duration (default: LENGTH_INDEFINITE)
     * @param onSnackbarShown Optional callback invoked when snackbar is shown
     * @param onSnackbarDismissed Optional callback invoked when snackbar is dismissed
     */
    fun showConnectionError(
        view: View,
        style: InAppChatStyle,
        duration: Int = Snackbar.LENGTH_INDEFINITE,
        onSnackbarShown: ((Snackbar) -> Unit)? = null,
        onSnackbarDismissed: (() -> Unit)? = null
    ) {
        val localizationUtils = LocalizationUtils.getInstance(view.context)
        val errorText = style.networkConnectionErrorTextRes
            ?.let { localizationUtils.getString(it) }
            ?: style.networkConnectionErrorText
            ?: view.context.getString(R.string.ib_chat_no_connection)

        runCatching {
            Snackbar.make(view, errorText, duration).apply {
                setBackgroundTint(style.networkConnectionErrorBackgroundColor)
                val textView = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                textView?.setTextColor(style.networkConnectionErrorTextColor)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && style.networkConnectionErrorTextAppearance != null) {
                    textView?.setTextAppearance(style.networkConnectionErrorTextAppearance)
                }


                if (style.networkConnectionErrorIcon != null) {
                    val icon = style.networkConnectionErrorIcon
                    DrawableCompat.setTint(icon, style.networkConnectionErrorIconTint)

                    // Scale down icon to 16dp
                    val scale = view.context.resources.displayMetrics.density
                    val iconSizeDp = 16
                    val iconSizePx = (iconSizeDp * scale + 0.5f).toInt()
                    icon.setBounds(0, 0, iconSizePx, iconSizePx)

                    textView?.setCompoundDrawablesRelative(icon, null, null, null)
                    val paddingDp = 8 // 8dp padding between icon and text
                    textView?.compoundDrawablePadding = (paddingDp * scale + 0.5f).toInt()
                }

                if (onSnackbarShown != null || onSnackbarDismissed != null) {
                    addCallback(object : Snackbar.Callback() {
                        override fun onShown(sb: Snackbar?) {
                            sb?.let { onSnackbarShown?.invoke(it) }
                        }

                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            onSnackbarDismissed?.invoke()
                        }
                    })
                }
            }.show()
        }.onFailure {
            MobileMessagingLogger.e("Failed to show connection error snackbar: ${it.message}", it)
        }
    }

    /**
     * Shows a snackbar styled for chat errors using snackbar error style properties.
     *
     * @param view The view to find a parent from for anchoring the snackbar
     * @param message The message to display in the snackbar
     * @param style The InAppChatStyle containing snackbar error styling
     * @param onSnackbarShown Optional callback invoked when snackbar is shown
     * @param onSnackbarDismissed Optional callback invoked when snackbar is dismissed
     */
    fun showChatError(
        view: View,
        message: String,
        style: InAppChatStyle,
        onSnackbarShown: ((Snackbar) -> Unit)? = null,
        onSnackbarDismissed: (() -> Unit)? = null
    ) {
        runCatching {
            Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).apply {
                style.chatSnackbarErrorBackgroundColor?.let { setBackgroundTint(it) }
                val textView = this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                style.chatSnackbarErrorTextColor?.let { textView?.setTextColor(it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && style.networkConnectionErrorTextAppearance != null) {
                    textView?.setTextAppearance(style.networkConnectionErrorTextAppearance)
                }
                textView?.maxLines = 4

                if (style.chatSnackbarErrorIcon != null) {
                    val icon = style.chatSnackbarErrorIcon
                    style.chatSnackbarErrorIconTint?.let { DrawableCompat.setTint(icon, it) }
                    textView?.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
                    val paddingDp = 8 // 8dp padding between icon and text
                    val scale = view.context.resources.displayMetrics.density
                    textView?.compoundDrawablePadding = (paddingDp * scale + 0.5f).toInt()
                }

                if (onSnackbarShown != null || onSnackbarDismissed != null) {
                    addCallback(object : Snackbar.Callback() {
                        override fun onShown(sb: Snackbar?) {
                            sb?.let { onSnackbarShown?.invoke(it) }
                        }

                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            onSnackbarDismissed?.invoke()
                        }
                    })
                }
            }.show()
        }.onFailure {
            MobileMessagingLogger.e("Failed to show chat error snackbar: ${it.message}", it)
        }
    }
}
