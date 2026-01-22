/*
 * PluginChatCustomization.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer
import org.infobip.mobile.messaging.chat.utils.toColorStateList
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.json.JSONException
import org.json.JSONObject

data class PluginChatToolbarCustomization(
    val titleTextAppearance: String? = null,
    val titleTextColor: String? = null,
    val titleText: String? = null,
    val titleCentered: Boolean = false,
    val backgroundColor: String? = null,
    val navigationIcon: String? = null,
    val navigationIconTint: String? = null,
    val subtitleTextAppearance: String? = null, // android only
    val subtitleTextColor: String? = null, // android only
    val subtitleText: String? = null, // android only
    val subtitleCentered: Boolean = false, // android only
)

data class PluginChatCustomization(
    val chatStatusBarBackgroundColor: String? = null,
    val chatStatusBarIconsColorMode: String? = null,
    // Toolbar
    val chatToolbar: PluginChatToolbarCustomization? = null,
    val attachmentPreviewToolbar: PluginChatToolbarCustomization? = null,
    val attachmentPreviewToolbarSaveMenuItemIcon: String? = null,
    val attachmentPreviewToolbarMenuItemsIconTint: String? = null,
    //Network error
    val networkErrorText: String? = null,
    val networkErrorTextColor: String? = null,
    val networkErrorTextAppearance: String? = null,
    val networkErrorLabelBackgroundColor: String? = null,
    val networkErrorIcon: String? = null,
    val networkErrorIconTint: String? = null,
    // Snackbar error
    val chatBannerErrorTextColor: String? = null,
    val chatBannerErrorTextAppearance: String? = null,
    val chatBannerErrorBackgroundColor: String? = null,
    val chatBannerErrorIcon: String? = null,
    val chatBannerErrorIconTint: String? = null,
    // Error Screen
    val chatErrorTitleText: String? = null,
    val chatErrorTitleTextColor: String? = null,
    val chatErrorTitleTextAppearance: String? = null,
    val chatErrorDescriptionText: String? = null,
    val chatErrorDescriptionTextColor: String? = null,
    val chatErrorDescriptionTextAppearance: String? = null,
    val chatErrorBackgroundColor: String? = null,
    val chatErrorIcon: String? = null,
    val chatErrorIconTint: String? = null,
    val chatErrorRefreshButtonText: String? = null,
    val chatErrorRefreshButtonTextColor: String? = null,
    val chatErrorRefreshButtonVisible: Boolean? = null,
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
    val chatInputSeparatorLineVisible: Boolean = false,
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
            .setIsTitleCentered(chatToolbar?.titleCentered)
            .setSubtitleTextAppearance(chatToolbar?.subtitleTextAppearance.toResId(context))
            .setSubtitleTextColor(chatToolbar?.subtitleTextColor.toColorIntOrNull())
            .setSubtitleText(chatToolbar?.subtitleText)
            .setIsSubtitleCentered(chatToolbar?.subtitleCentered)
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
            .setIsTitleCentered(attachmentPreviewToolbar?.titleCentered)
            .setSubtitleTextAppearance(attachmentPreviewToolbar?.subtitleTextAppearance.toResId(context))
            .setSubtitleTextColor(attachmentPreviewToolbar?.subtitleTextColor.toColorIntOrNull())
            .setSubtitleText(attachmentPreviewToolbar?.subtitleText)
            .setIsSubtitleCentered(attachmentPreviewToolbar?.subtitleCentered)
            .build()

        val chatStyle = InAppChatStyle.Builder()
            .setBackgroundColor(chatBackgroundColor.toColorIntOrNull())
            .setProgressBarColor(chatProgressBarColor.toColorIntOrNull())
            .setNetworkConnectionErrorText(networkErrorText)
            .setNetworkConnectionErrorTextColor(networkErrorTextColor.toColorIntOrNull())
            .setNetworkConnectionErrorTextAppearance(networkErrorTextAppearance.toResId(context))
            .setNetworkConnectionErrorBackgroundColor(networkErrorLabelBackgroundColor.toColorIntOrNull())
            .setNetworkConnectionErrorIcon(networkErrorIcon.toDrawable(context, drawableLoader))
            .setNetworkConnectionErrorIconTint(networkErrorIconTint.toColorIntOrNull())
            .setChatSnackbarErrorTextColor(chatBannerErrorTextColor.toColorIntOrNull())
            .setChatSnackbarErrorTextAppearance(chatBannerErrorTextAppearance.toResId(context))
            .setChatSnackbarErrorBackgroundColor(chatBannerErrorBackgroundColor.toColorIntOrNull())
            .setChatSnackbarErrorIcon(chatBannerErrorIcon.toDrawable(context, drawableLoader))
            .setChatSnackbarErrorIconTint(chatBannerErrorIconTint.toColorIntOrNull())
            .setChatFullScreenErrorTitleText(chatErrorTitleText)
            .setChatFullScreenErrorTitleTextColor(chatErrorTitleTextColor.toColorIntOrNull())
            .setChatFullScreenErrorTitleTextAppearance(chatErrorTitleTextAppearance.toResId(context))
            .setChatFullScreenErrorDescriptionText(chatErrorDescriptionText)
            .setChatFullScreenErrorDescriptionTextColor(chatErrorDescriptionTextColor.toColorIntOrNull())
            .setChatFullScreenErrorDescriptionTextAppearance(chatErrorDescriptionTextAppearance.toResId(context))
            .setChatFullScreenErrorBackgroundColor(chatErrorBackgroundColor.toColorIntOrNull())
            .setChatFullScreenErrorIcon(chatErrorIcon.toDrawable(context, drawableLoader))
            .setChatFullScreenErrorIconTint(chatErrorIconTint.toColorIntOrNull())
            .setChatFullScreenErrorRefreshButtonText(chatErrorRefreshButtonText)
            .setChatFullScreenErrorRefreshButtonTextColor(chatErrorRefreshButtonTextColor.toColorIntOrNull())
            .setChatFullScreenErrorRefreshButtonVisible(chatErrorRefreshButtonVisible)
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
            .setIsSeparatorLineVisible(chatInputSeparatorLineVisible)
            .setCursorColor(chatInputCursorColor.toColorIntOrNull())
            .setCharCounterTextAppearance(chatInputCharCounterTextAppearance.toResId(context))
            .setCharCounterDefaultColor(chatInputCharCounterDefaultColor.toColorIntOrNull())
            .setCharCounterAlertColor(chatInputCharCounterAlertColor.toColorIntOrNull())
            .setAttachmentIconTint(chatInputAttachmentIconTint.toColorIntOrNull().toColorStateList())
            .setSendIconTint(chatInputSendIconTint.toColorIntOrNull().toColorStateList())
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
