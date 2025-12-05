/*
 * PluginChatCustomization.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.json.JSONException
import org.json.JSONObject

data class PluginChatToolbarCustomization(
    val titleTextAppearance: String? = null,
    val titleTextColor: String? = null,
    val titleText: String? = null,
    val isTitleCentered: Boolean = false,
    val backgroundColor: String? = null,
    val navigationIcon: String? = null,
    val navigationIconTint: String? = null,
    val subtitleTextAppearance: String? = null, // android only
    val subtitleTextColor: String? = null, // android only
    val subtitleText: String? = null, // android only
    val isSubtitleCentered: Boolean = false, // android only
)

data class PluginChatCustomization(
    val chatStatusBarBackgroundColor: String? = null,
    val chatStatusBarIconsColorMode: String? = null,
    // Toolbar
    val chatToolbar: PluginChatToolbarCustomization? = null,
    val attachmentPreviewToolbar: PluginChatToolbarCustomization? = null,
    val attachmentPreviewToolbarSaveMenuItemIcon: String? = null,
    val attachmentPreviewToolbarMenuItemsIconTint: String? = null,
    // NetworkError
    val networkErrorText: String? = null,
    val networkErrorTextColor: String? = null,
    val networkErrorTextAppearance: String? = null,
    val networkErrorLabelBackgroundColor: String? = null,
    // Chat
    val chatBackgroundColor: String? = null,
    val chatProgressBarColor: String? = null,
    // Input
    val chatInputTextAppearance: String? = null,
    val chatInputTextColor: String? = null,
    val chatInputBackgroundColor: String? = null,
    val chatInputHintText: String? = null,
    val chatInputHintTextColor: String? = null,
    val chatInputAttachmentIcon: String? = null,
    val chatInputAttachmentIconTint: String? = null,
    val chatInputAttachmentBackgroundDrawable: String? = null,
    val chatInputAttachmentBackgroundColor: String? = null,
    val chatInputSendIcon: String? = null,
    val chatInputSendIconTint: String? = null,
    val chatInputSendBackgroundDrawable: String? = null,
    val chatInputSendBackgroundColor: String? = null,
    val chatInputSeparatorLineColor: String? = null,
    val isChatInputSeparatorLineVisible: Boolean = false,
    val chatInputCursorColor: String? = null,
    val chatInputCharCounterTextAppearance: String? = null,
    val chatInputCharCounterDefaultColor: String? = null,
    val chatInputCharCounterAlertColor: String? = null,
) {
    interface DrawableLoader {
        fun loadDrawable(context: Context, drawableSrc: String?): Drawable?
    }

    companion object {
        private const val TAG = "PluginChatTheme"

        fun parseOrNull(jsonString: String?): PluginChatCustomization? {
            return try {
                jsonString?.let {
                    JsonSerializer().deserialize(it, PluginChatCustomization::class.java)
                }
            } catch (e: JSONException) {
                MobileMessagingLogger.e(TAG, "parse($jsonString)", e)
                null
            }
        }

        @Throws(JSONException::class)
        fun parseOrNull(jsonObject: JSONObject?): PluginChatCustomization? = parseOrNull(jsonObject?.toString())
    }

    fun createTheme(context: Context?, drawableLoader: DrawableLoader?): InAppChatTheme {
        val chatToolbarStyle = InAppChatToolbarStyle.Builder()
            .setStatusBarBackgroundColor(chatStatusBarBackgroundColor.toColorIntOrNull())
            .setLightStatusBarIcons(chatStatusBarIconsColorMode === "light")
            .setToolbarBackgroundColor(chatToolbar?.backgroundColor.toColorIntOrNull())
            .setNavigationIcon(chatToolbar?.navigationIcon?.toDrawable(context, drawableLoader))
            .setNavigationIconTint(chatToolbar?.navigationIconTint.toColorIntOrNull())
            .setTitleTextAppearance(chatToolbar?.titleTextAppearance.toResId(context))
            .setTitleTextColor(chatToolbar?.titleTextColor.toColorIntOrNull())
            .setTitleText(chatToolbar?.titleText)
            .setIsTitleCentered(chatToolbar?.isTitleCentered)
            .setSubtitleTextAppearance(chatToolbar?.subtitleTextAppearance.toResId(context))
            .setSubtitleTextColor(chatToolbar?.subtitleTextColor.toColorIntOrNull())
            .setSubtitleText(chatToolbar?.subtitleText)
            .setIsSubtitleCentered(chatToolbar?.isSubtitleCentered)
            .build()

        val attachmentPreviewToolbarStyle = InAppChatToolbarStyle.Builder()
            .setStatusBarBackgroundColor(chatStatusBarBackgroundColor.toColorIntOrNull())
            .setLightStatusBarIcons(chatStatusBarIconsColorMode === "light")
            .setToolbarBackgroundColor(attachmentPreviewToolbar?.backgroundColor.toColorIntOrNull())
            .setNavigationIcon(attachmentPreviewToolbar?.navigationIcon.toDrawable(context, drawableLoader))
            .setNavigationIconTint(attachmentPreviewToolbar?.navigationIconTint.toColorIntOrNull())
            .setSaveAttachmentMenuItemIcon(attachmentPreviewToolbarSaveMenuItemIcon.toDrawable(context, drawableLoader))
            .setMenuItemsIconTint(attachmentPreviewToolbarMenuItemsIconTint.toColorIntOrNull())
            .setTitleTextAppearance(attachmentPreviewToolbar?.titleTextAppearance.toResId(context))
            .setTitleTextColor(attachmentPreviewToolbar?.titleTextColor.toColorIntOrNull())
            .setTitleText(attachmentPreviewToolbar?.titleText)
            .setIsTitleCentered(attachmentPreviewToolbar?.isTitleCentered)
            .setSubtitleTextAppearance(attachmentPreviewToolbar?.subtitleTextAppearance.toResId(context))
            .setSubtitleTextColor(attachmentPreviewToolbar?.subtitleTextColor.toColorIntOrNull())
            .setSubtitleText(attachmentPreviewToolbar?.subtitleText)
            .setIsSubtitleCentered(attachmentPreviewToolbar?.isSubtitleCentered)
            .build()

        val chatStyle = InAppChatStyle.Builder()
            .setBackgroundColor(chatBackgroundColor.toColorIntOrNull())
            .setProgressBarColor(chatProgressBarColor.toColorIntOrNull())
            .setNetworkConnectionText(networkErrorText)
            .setNetworkConnectionTextAppearance(networkErrorTextAppearance.toResId(context))
            .setNetworkConnectionTextColor(networkErrorTextColor.toColorIntOrNull())
            .setNetworkConnectionLabelBackgroundColor(networkErrorLabelBackgroundColor.toColorIntOrNull())
            .build()

        val inputViewStyle = InAppChatInputViewStyle.Builder()
            .setTextAppearance(chatInputTextAppearance.toResId(context))
            .setTextColor(chatInputTextColor.toColorIntOrNull())
            .setBackgroundColor(chatInputBackgroundColor.toColorIntOrNull())
            .setHintText(chatInputHintText)
            .setHintTextColor(chatInputHintTextColor.toColorIntOrNull())
            .setAttachmentIcon(chatInputAttachmentIcon.toDrawable(context, drawableLoader))
            .setAttachmentBackgroundDrawable(chatInputAttachmentBackgroundDrawable.toDrawable(context, drawableLoader))
            .setAttachmentBackgroundColor(chatInputAttachmentBackgroundColor.toColorIntOrNull())
            .setSendIcon(chatInputSendIcon.toDrawable(context, drawableLoader))
            .setSendBackgroundDrawable(chatInputSendBackgroundDrawable.toDrawable(context, drawableLoader))
            .setSendBackgroundColor(chatInputSendBackgroundColor.toColorIntOrNull())
            .setSeparatorLineColor(chatInputSeparatorLineColor.toColorIntOrNull())
            .setIsSeparatorLineVisible(isChatInputSeparatorLineVisible)
            .setCursorColor(chatInputCursorColor.toColorIntOrNull())
            .setCharCounterTextAppearance(chatInputCharCounterTextAppearance.toResId(context))
            .setCharCounterDefaultColor(chatInputCharCounterDefaultColor.toColorIntOrNull())
            .setCharCounterAlertColor(chatInputCharCounterAlertColor.toColorIntOrNull())
            .setAttachmentIconTint(chatInputAttachmentIconTint.toColorIntOrNull()?.let { ColorStateList.valueOf(it) })
            .setSendIconTint(chatInputSendIconTint.toColorIntOrNull()?.let { ColorStateList.valueOf(it) })
            .build()

        return InAppChatTheme(
            chatToolbarStyle,
            attachmentPreviewToolbarStyle,
            chatStyle,
            inputViewStyle
        )
    }

    @ColorInt
    private fun String?.toColorIntOrNull(): Int? {
        return try {
            this?.toColorInt()
        } catch (e: IllegalArgumentException) {
            MobileMessagingLogger.e(TAG, "toColorIntOrNull($this)", e)
            null
        }
    }

    private fun String?.toResId(context: Context?): Int? {
        if (this.isNullOrBlank() || context == null) return null
        val resPath = this
        return try {
            val resources = context.resources
            val packageName = context.packageName
            var resId = resources.getIdentifier(resPath, "mipmap", packageName)
            if (resId == 0) {
                resId = resources.getIdentifier(resPath, "drawable", packageName)
            }
            if (resId == 0) {
                resId = resources.getIdentifier(resPath, "raw", packageName)
            }
            if (resId == 0) {
                resId = resources.getIdentifier(resPath, "style", packageName)
            }
            resId
        } catch (e: Exception) {
            MobileMessagingLogger.e(TAG, "toResId($resPath)", e)
            null
        }
    }

    private fun String?.toDrawable(context: Context?, drawableLoader: DrawableLoader?): Drawable? {
        if (this.isNullOrBlank() || context == null || drawableLoader == null) return null
        return try {
            drawableLoader.loadDrawable(context, this)
        } catch (e: Exception) {
            MobileMessagingLogger.e(TAG, "toDrawable($this)", e)
            null
        }
    }

}
