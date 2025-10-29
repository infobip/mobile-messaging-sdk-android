/*
 * InAppChatToolbarStyle.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.res.getBooleanOrThrow
import com.google.android.material.appbar.MaterialToolbar
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils
import org.infobip.mobile.messaging.chat.utils.colorBackground
import org.infobip.mobile.messaging.chat.utils.colorPrimary
import org.infobip.mobile.messaging.chat.utils.colorPrimaryDark
import org.infobip.mobile.messaging.chat.utils.colorPrimaryText
import org.infobip.mobile.messaging.chat.utils.getDrawableCompat
import org.infobip.mobile.messaging.chat.utils.isAttributePresent
import org.infobip.mobile.messaging.chat.utils.isIbDefaultTheme
import org.infobip.mobile.messaging.chat.utils.resolveStringWithResId
import org.infobip.mobile.messaging.chat.utils.takeIfDefined

data class InAppChatToolbarStyle @JvmOverloads constructor(
    @ColorInt val toolbarBackgroundColor: Int = Defaults.toolbarBackgroundColor,
    @ColorInt val statusBarBackgroundColor: Int = Defaults.statusBarBackgroundColor,
    val lightStatusBarIcons: Boolean = Defaults.lightStatusBarIcons,
    val navigationIcon: Drawable? = null,
    @ColorInt val navigationIconTint: Int = Defaults.navigationIconTint,
    val saveAttachmentMenuItemIcon: Drawable? = null,
    @ColorInt val menuItemsIconTint: Int = Defaults.menuItemsIconTint,
    @StyleRes val titleTextAppearance: Int? = null,
    @ColorInt val titleTextColor: Int = Defaults.titleTextColor,
    val titleText: String? = null,
    @StringRes val titleTextRes: Int? = null,
    val isTitleCentered: Boolean = Defaults.isTitleCentered,
    @StyleRes val subtitleTextAppearance: Int? = null,
    @ColorInt val subtitleTextColor: Int = Defaults.subtitleTextColor,
    val subtitleText: String? = null,
    @StringRes val subtitleTextRes: Int? = null,
    val isSubtitleCentered: Boolean = Defaults.isSubtitleCentered
) {
    object Defaults {
        @ColorInt val toolbarBackgroundColor: Int = Color.BLACK
        @ColorInt val statusBarBackgroundColor: Int = Color.BLACK
        const val lightStatusBarIcons: Boolean = true
        @ColorInt val navigationIconTint: Int = Color.WHITE
        @ColorInt val menuItemsIconTint: Int = Color.WHITE
        @ColorInt val titleTextColor: Int = Color.WHITE
        const val isTitleCentered: Boolean = false
        @ColorInt val subtitleTextColor: Int = Color.WHITE
        const val isSubtitleCentered: Boolean = false
    }

    class Builder {
        private var toolbarBackgroundColor: Int = Defaults.toolbarBackgroundColor
        private var statusBarBackgroundColor: Int = Defaults.statusBarBackgroundColor
        private var lightStatusBarIcons: Boolean = Defaults.lightStatusBarIcons
        private var navigationIcon: Drawable? = null
        private var navigationIconTint: Int = Defaults.navigationIconTint
        private var saveAttachmentMenuItemIcon: Drawable? = null
        private var menuItemsIconTint: Int = Defaults.menuItemsIconTint
        private var titleTextAppearance: Int? = null
        private var titleTextColor: Int = Defaults.titleTextColor
        private var titleText: String? = null
        private var titleTextRes: Int? = null
        private var isTitleCentered: Boolean = Defaults.isTitleCentered
        private var subtitleTextAppearance: Int? = null
        private var subtitleTextColor: Int = Defaults.subtitleTextColor
        private var subtitleText: String? = null
        private var subtitleTextRes: Int? = null
        private var isSubtitleCentered: Boolean = Defaults.isSubtitleCentered

        fun setToolbarBackgroundColor(@ColorInt toolbarBackgroundColor: Int?) = apply { toolbarBackgroundColor?.let { this.toolbarBackgroundColor = it } }
        fun setStatusBarBackgroundColor(@ColorInt statusBarBackgroundColor: Int?) = apply { statusBarBackgroundColor?.let { this.statusBarBackgroundColor = it } }
        fun setLightStatusBarIcons(lightStatusBarIcons: Boolean?) = apply { lightStatusBarIcons?.let { this.lightStatusBarIcons = it } }
        fun setNavigationIcon(navigationIcon: Drawable?) = apply { navigationIcon?.let { this.navigationIcon = it } }
        fun setNavigationIconTint(@ColorInt navigationIconTint: Int?) = apply { navigationIconTint?.let { this.navigationIconTint = it } }
        fun setSaveAttachmentMenuItemIcon(saveAttachmentMenuItemIcon: Drawable?) = apply { saveAttachmentMenuItemIcon?.let { this.saveAttachmentMenuItemIcon = it } }
        fun setMenuItemsIconTint(@ColorInt menuItemsIconTint: Int?) = apply { menuItemsIconTint?.let { this.menuItemsIconTint = it } }
        fun setTitleTextAppearance(@StyleRes titleTextAppearance: Int?) = apply { titleTextAppearance?.let { this.titleTextAppearance = it } }
        fun setTitleTextColor(@ColorInt titleTextColor: Int?) = apply { titleTextColor?.let { this.titleTextColor = it } }
        fun setTitleText(titleText: String?) = apply { titleText?.let { this.titleText = it } }
        fun setTitleTextRes(@StringRes titleTextRes: Int?) = apply { titleTextRes?.let { this.titleTextRes = it } }
        fun setIsTitleCentered(isTitleCentered: Boolean?) = apply { isTitleCentered?.let { this.isTitleCentered = it } }
        fun setSubtitleTextAppearance(@StyleRes subtitleTextAppearance: Int?) = apply { subtitleTextAppearance?.let { this.subtitleTextAppearance = it } }
        fun setSubtitleTextColor(@ColorInt subtitleTextColor: Int?) = apply { subtitleTextColor?.let { this.subtitleTextColor = it } }
        fun setSubtitleText(subtitleText: String?) = apply { subtitleText?.let { this.subtitleText = it } }
        fun setSubtitleTextRes(@StringRes subtitleTextRes: Int?) = apply { subtitleTextRes?.let { this.subtitleTextRes = it } }
        fun setIsSubtitleCentered(isSubtitleCentered: Boolean?) = apply { isSubtitleCentered?.let { this.isSubtitleCentered = it } }

        fun build() = InAppChatToolbarStyle(
            toolbarBackgroundColor = toolbarBackgroundColor,
            statusBarBackgroundColor = statusBarBackgroundColor,
            lightStatusBarIcons = lightStatusBarIcons,
            navigationIcon = navigationIcon,
            navigationIconTint = navigationIconTint,
            saveAttachmentMenuItemIcon = saveAttachmentMenuItemIcon,
            menuItemsIconTint = menuItemsIconTint,
            titleTextAppearance = titleTextAppearance,
            titleTextColor = titleTextColor,
            titleText = titleText,
            titleTextRes = titleTextRes,
            isTitleCentered = isTitleCentered,
            subtitleTextAppearance = subtitleTextAppearance,
            subtitleTextColor = subtitleTextColor,
            subtitleText = subtitleText,
            subtitleTextRes = subtitleTextRes,
            isSubtitleCentered = isSubtitleCentered
        )
    }

    companion object {

        private operator fun invoke(@AttrRes attr: Int, context: Context): InAppChatToolbarStyle {
            val theme = context.theme

            //load new style values
            var toolbarBackgroundColor: Int? = null
            var statusBarBackgroundColor: Int? = null
            var lightStatusBarIcons: Boolean? = null
            var navigationIcon: Int? = null
            var navigationIconTint: Int? = null
            var saveAttachmentMenuItemIcon: Int? = null
            var menuItemsIconTint: Int? = null
            var titleTextAppearance: Int? = null
            var titleTextColor: Int? = null
            var titleText: String? = null
            var titleTextRes: Int? = null
            var isTitleCentered: Boolean? = null
            var subtitleTextAppearance: Int? = null
            var subtitleTextColor: Int? = null
            var subtitleText: String? = null
            var subtitleTextRes: Int? = null
            var isSubtitleCentered: Boolean? = null

            val typedValue = TypedValue()
            theme.resolveAttribute(attr, typedValue, true)
            if (typedValue.data != 0) {
                val typedArray: TypedArray = theme.obtainStyledAttributes(typedValue.data, R.styleable.InAppChatToolbarViewStyleable)
                toolbarBackgroundColor = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatToolbarBackgroundColor, 0).takeIfDefined()
                statusBarBackgroundColor = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatStatusBarBackgroundColor, 0).takeIfDefined()
                lightStatusBarIcons = typedArray.getInt(R.styleable.InAppChatToolbarViewStyleable_ibChatStatusBarIconsColorMode, 0) == 0
                navigationIcon = typedArray.getResourceId(R.styleable.InAppChatToolbarViewStyleable_ibChatNavigationIcon, 0).takeIfDefined()
                navigationIconTint = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatNavigationIconTint, 0).takeIfDefined()
                saveAttachmentMenuItemIcon = typedArray.getResourceId(R.styleable.InAppChatToolbarViewStyleable_ibChatSaveAttachmentMenuItemIcon, 0).takeIfDefined()
                menuItemsIconTint = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatMenuItemsIconTint, 0).takeIfDefined()
                titleTextAppearance = typedArray.getResourceId(R.styleable.InAppChatToolbarViewStyleable_ibChatTitleTextAppearance, 0).takeIfDefined()
                titleTextColor = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatTitleTextColor, 0).takeIfDefined()
                typedArray.resolveStringWithResId(context, R.styleable.InAppChatToolbarViewStyleable_ibChatTitleText).let {
                        titleTextRes = it.first
                        titleText = it.second
                }
                isTitleCentered = runCatching { typedArray.getBooleanOrThrow(R.styleable.InAppChatToolbarViewStyleable_ibChatTitleCentered) }.getOrNull()
                subtitleTextAppearance = typedArray.getResourceId(R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleTextAppearance, 0).takeIfDefined()
                subtitleTextColor = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleTextColor, 0).takeIfDefined()
                typedArray.resolveStringWithResId(context, R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleText).let {
                    subtitleTextRes = it.first
                    subtitleText = it.second
                }
                isSubtitleCentered = runCatching { typedArray.getBooleanOrThrow(R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleCentered) }.getOrNull()
                typedArray.recycle()
            }

            return InAppChatToolbarStyle(
                    toolbarBackgroundColor = toolbarBackgroundColor ?: Defaults.toolbarBackgroundColor,
                    statusBarBackgroundColor = statusBarBackgroundColor ?: Defaults.statusBarBackgroundColor,
                    lightStatusBarIcons = lightStatusBarIcons ?: Defaults.lightStatusBarIcons,
                    navigationIcon = (navigationIcon ?: R.drawable.ic_chat_arrow_back).let(context::getDrawableCompat),
                    navigationIconTint = navigationIconTint ?: Defaults.navigationIconTint,
                    saveAttachmentMenuItemIcon = (saveAttachmentMenuItemIcon ?: R.drawable.ib_chat_attachment_save_btn_icon).let(context::getDrawableCompat),
                    menuItemsIconTint = menuItemsIconTint ?: Defaults.menuItemsIconTint,
                    titleTextAppearance = titleTextAppearance,
                    titleTextColor = titleTextColor ?: Defaults.titleTextColor,
                    titleText = titleText,
                    titleTextRes = titleTextRes,
                    isTitleCentered = isTitleCentered ?: Defaults.isTitleCentered,
                    subtitleTextAppearance = subtitleTextAppearance,
                    subtitleTextColor = subtitleTextColor ?: Defaults.subtitleTextColor,
                    subtitleText = subtitleText,
                    subtitleTextRes = subtitleTextRes,
                    isSubtitleCentered = isSubtitleCentered ?: Defaults.isSubtitleCentered,
            )

        }

        private fun prepareStyle(@AttrRes attr: Int, context: Context, widgetInfo: WidgetInfo?): InAppChatToolbarStyle {
            //theme config
            var style = invoke(attr, context)
            val theme = context.theme
            val isIbDefaultTheme = theme.isIbDefaultTheme()

            @ColorInt
            val colorPrimary = widgetInfo?.colorPrimary

            @ColorInt
            val colorBackground = widgetInfo?.colorBackground

            @ColorInt
            val colorPrimaryDark = widgetInfo?.colorPrimaryDark

            @ColorInt
            val colorPrimaryText = widgetInfo?.colorPrimaryText

            if (isIbDefaultTheme) { //if it is IB default theme apply widget color automatically to all components
                if (colorPrimary != null) {
                    style = style.copy(toolbarBackgroundColor = colorPrimary)
                }
                if (colorPrimaryDark != null) {
                    style = style.copy(statusBarBackgroundColor = colorPrimaryDark)
                }
                if (colorPrimaryText != null) {
                    style = style.copy(titleTextColor = colorPrimaryText, subtitleTextColor = colorPrimaryText)
                }
                if (colorBackground != null) {
                    style = style.copy(
                        titleTextColor = colorBackground,
                        subtitleTextColor = colorBackground,
                        navigationIconTint = colorBackground,
                        menuItemsIconTint = colorBackground,
                    )
                }
            } else { //if it is theme provided by integrator apply widget color only to components which are not defined by integrator
                val backgroundColorDefined = theme.isAttributePresent(
                        R.styleable.InAppChatToolbarViewStyleable_ibChatToolbarBackgroundColor,
                        attr,
                        R.styleable.InAppChatToolbarViewStyleable
                )
                if (!backgroundColorDefined && colorPrimary != null) {
                    style = style.copy(toolbarBackgroundColor = colorPrimary)
                }

                val statusBarBackgroundColorDefined = theme.isAttributePresent(
                        R.styleable.InAppChatToolbarViewStyleable_ibChatStatusBarBackgroundColor,
                        attr,
                        R.styleable.InAppChatToolbarViewStyleable
                )
                if (!statusBarBackgroundColorDefined && colorPrimaryDark != null) {
                    style = style.copy(statusBarBackgroundColor = colorPrimaryDark)
                }

                val titleTextColorDefined = theme.isAttributePresent(
                        R.styleable.InAppChatToolbarViewStyleable_ibChatTitleTextColor,
                        attr,
                        R.styleable.InAppChatToolbarViewStyleable
                )
                if (!titleTextColorDefined && colorPrimaryText != null) {
                    style = style.copy(titleTextColor = colorPrimaryText)
                }

                val newSubtitleTextColorDefined = theme.isAttributePresent(
                        R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleTextColor,
                        attr,
                        R.styleable.InAppChatToolbarViewStyleable
                )
                if (!newSubtitleTextColorDefined && colorPrimaryText != null) {
                    style = style.copy(subtitleTextColor = colorPrimaryText)
                }

                val newNavigationIconTintDefined = theme.isAttributePresent(R.styleable.InAppChatToolbarViewStyleable_ibChatNavigationIconTint, attr, R.styleable.InAppChatToolbarViewStyleable)
                if (!newNavigationIconTintDefined && colorPrimaryText != null) {
                    style = style.copy(navigationIconTint = colorPrimaryText)
                }

                val newMenuItemsIconTintDefined = theme.isAttributePresent(R.styleable.InAppChatToolbarViewStyleable_ibChatMenuItemsIconTint, attr, R.styleable.InAppChatToolbarViewStyleable)
                if (!newMenuItemsIconTintDefined && colorPrimaryText != null) {
                    style = style.copy(menuItemsIconTint = colorPrimaryText)
                }
            }

            if (style.titleText.isNullOrBlank() && widgetInfo?.title?.isNotBlank() == true) {
                style = style.copy(titleText = widgetInfo.title, titleTextRes = null)
            }

            return style
        }

        /**
         * Creates [InAppChatToolbarStyle] from android theme "IB_AppTheme.Chat" defined by integrator and [WidgetInfo] livechat widget configuration.
         * If "IB_AppTheme.Chat" does not exists in application, then online fetched [WidgetInfo] is used.
         * If [WidgetInfo] could not be fetched, then default IB theme "IB_ChatDefaultTheme.Styled" as last option.
         * Priority: IB_AppTheme.Chat > [WidgetInfo] > IB_ChatDefaultTheme
         */
        @JvmStatic
        fun createChatToolbarStyle(context: Context, widgetInfo: WidgetInfo?): InAppChatToolbarStyle = prepareStyle(R.attr.ibChatToolbarStyle, context, widgetInfo)

        /**
         * Creates [InAppChatToolbarStyle] from android theme "IB_AppTheme.Chat" defined by integrator and [WidgetInfo] livechat widget configuration.
         * If "IB_AppTheme.Chat" does not exists in application, then online fetched [WidgetInfo] is used.
         * If [WidgetInfo] could not be fetched, then default IB theme "IB_ChatDefaultTheme.Styled" as last option.
         * Priority: IB_AppTheme.Chat > [WidgetInfo] > IB_ChatDefaultTheme
         */
        @JvmStatic
        fun createChatAttachmentStyle(context: Context, widgetInfo: WidgetInfo?): InAppChatToolbarStyle = prepareStyle(R.attr.ibChatAttachmentToolbarStyle, context, widgetInfo)
    }

}

internal fun InAppChatToolbarStyle.apply(toolbar: MaterialToolbar?) {
    toolbar?.let {
        val localizationUtils = LocalizationUtils.getInstance(it.context)
        it.navigationIcon = navigationIcon ?: it.context.getDrawableCompat(R.drawable.ic_chat_arrow_back)
        it.setNavigationIconTint(navigationIconTint)
        it.setBackgroundColor(toolbarBackgroundColor)
        if (titleTextRes != null) {
            it.title = localizationUtils.getString(titleTextRes)
        } else if (titleText != null) {
            it.title = titleText
        }
        titleTextAppearance?.let { appearance ->
            toolbar.setTitleTextAppearance(
                    toolbar.context,
                    appearance
            )
        }
        it.setTitleTextColor(titleTextColor)
        isTitleCentered.let { isCentered -> it.isTitleCentered = isCentered }
        if (subtitleTextRes != null) {
            it.subtitle = localizationUtils.getString(subtitleTextRes)
        } else if (subtitleText != null) {
            it.subtitle = subtitleText
        }
        subtitleTextAppearance?.let { appearance ->
            toolbar.setSubtitleTextAppearance(
                    toolbar.context,
                    appearance
            )
        }
        it.setSubtitleTextColor(subtitleTextColor)
        isSubtitleCentered.let { isCentered -> it.isSubtitleCentered = isCentered }
    }
}