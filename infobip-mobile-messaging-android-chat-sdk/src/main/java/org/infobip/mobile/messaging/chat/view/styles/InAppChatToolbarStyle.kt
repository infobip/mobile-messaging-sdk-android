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
import org.infobip.mobile.messaging.chat.utils.getDrawableCompat
import org.infobip.mobile.messaging.chat.utils.isAttributePresent
import org.infobip.mobile.messaging.chat.utils.isIbDefaultTheme
import org.infobip.mobile.messaging.chat.utils.isMMBaseTheme
import org.infobip.mobile.messaging.chat.utils.resolveStringWithResId
import org.infobip.mobile.messaging.chat.utils.resolveThemeColor
import org.infobip.mobile.messaging.chat.utils.takeIfDefined
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

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

        private const val RES_ID_CHAT_VIEW_TITLE = "ib_in_app_chat_view_title"

        private operator fun invoke(@AttrRes attr: Int, context: Context): InAppChatToolbarStyle {
            val theme = context.theme

            //load deprecated attributes
            val deprecatedToolbarBackgroundColor = context.resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)
            val deprecatedStatusBarBackgroundColor = context.resolveThemeColor(androidx.appcompat.R.attr.colorPrimaryDark)
            val deprecatedTitleTextColor = context.resolveThemeColor(androidx.appcompat.R.attr.titleTextColor)
            val deprecatedToolbarIconTint = context.resolveThemeColor(androidx.appcompat.R.attr.colorControlNormal)
            val deprecatedTitleResId: Int = runCatching {
                context.resources.getIdentifier(
                        RES_ID_CHAT_VIEW_TITLE,
                        "string",
                        context.applicationContext.packageName
                ).takeIfDefined()
            }.onFailure {
                MobileMessagingLogger.e("Can't load resource: $RES_ID_CHAT_VIEW_TITLE", it)
            }.getOrNull() ?: R.string.ib_chat_view_title
            val deprecatedTitle = context.getString(deprecatedTitleResId)

            //load new style values
            var newToolbarBackgroundColor: Int? = null
            var newStatusBarBackgroundColor: Int? = null
            var lightStatusBarIcons: Boolean? = null
            var newNavigationIcon: Int? = null
            var newNavigationIconTint: Int? = null
            var newSaveAttachmentMenuItemIcon: Int? = null
            var newMenuItemsIconTint: Int? = null
            var newTitleTextAppearance: Int? = null
            var newTitleTextColor: Int? = null
            var newTitleText: String? = null
            var newTitleTextRes: Int? = null
            var newIsTitleCentered = false
            var newSubtitleTextAppearance: Int? = null
            var newSubtitleTextColor: Int? = null
            var newSubtitleText: String? = null
            var newSubtitleTextRes: Int? = null
            var newIsSubtitleCentered = false

            val typedValue = TypedValue()
            theme.resolveAttribute(attr, typedValue, true)
            if (typedValue.data != 0) {
                val typedArray: TypedArray = theme.obtainStyledAttributes(typedValue.data, R.styleable.InAppChatToolbarViewStyleable)
                newToolbarBackgroundColor = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatToolbarBackgroundColor, 0).takeIfDefined()
                newStatusBarBackgroundColor = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatStatusBarBackgroundColor, 0).takeIfDefined()
                lightStatusBarIcons = typedArray.getInt(R.styleable.InAppChatToolbarViewStyleable_ibChatStatusBarIconsColorMode, 0) == 0
                newNavigationIcon = typedArray.getResourceId(R.styleable.InAppChatToolbarViewStyleable_ibChatNavigationIcon, 0).takeIfDefined()
                newNavigationIconTint = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatNavigationIconTint, 0).takeIfDefined()
                newSaveAttachmentMenuItemIcon = typedArray.getResourceId(R.styleable.InAppChatToolbarViewStyleable_ibChatSaveAttachmentMenuItemIcon, 0).takeIfDefined()
                newMenuItemsIconTint = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatMenuItemsIconTint, 0).takeIfDefined()
                newTitleTextAppearance = typedArray.getResourceId(R.styleable.InAppChatToolbarViewStyleable_ibChatTitleTextAppearance, 0).takeIfDefined()
                newTitleTextColor = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatTitleTextColor, 0).takeIfDefined()
                typedArray.resolveStringWithResId(context, R.styleable.InAppChatToolbarViewStyleable_ibChatTitleText).let {
                        newTitleTextRes = it.first
                        newTitleText = it.second
                }
                newIsTitleCentered = runCatching { typedArray.getBooleanOrThrow(R.styleable.InAppChatToolbarViewStyleable_ibChatTitleCentered) }.getOrDefault(false)
                newSubtitleTextAppearance = typedArray.getResourceId(R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleTextAppearance, 0).takeIfDefined()
                newSubtitleTextColor = typedArray.getColor(R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleTextColor, 0).takeIfDefined()
                typedArray.resolveStringWithResId(context, R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleText).let {
                    newSubtitleTextRes = it.first
                    newSubtitleText = it.second
                }
                newIsSubtitleCentered = runCatching { typedArray.getBooleanOrThrow(R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleCentered) }.getOrDefault(false)
                typedArray.recycle()
            }

            return InAppChatToolbarStyle(
                    toolbarBackgroundColor = newToolbarBackgroundColor ?: deprecatedToolbarBackgroundColor ?: Color.BLACK,
                    statusBarBackgroundColor = newStatusBarBackgroundColor ?: deprecatedStatusBarBackgroundColor ?: Color.BLACK,
                    lightStatusBarIcons = lightStatusBarIcons ?: true,
                    navigationIcon = (newNavigationIcon ?: R.drawable.ic_chat_arrow_back).let(context::getDrawableCompat),
                    navigationIconTint = newNavigationIconTint ?: deprecatedToolbarIconTint ?: Color.WHITE,
                    saveAttachmentMenuItemIcon = (newSaveAttachmentMenuItemIcon ?: R.drawable.ib_chat_attachment_save_btn_icon).let(context::getDrawableCompat),
                    menuItemsIconTint = newMenuItemsIconTint ?: deprecatedToolbarIconTint ?: Color.WHITE,
                    titleTextAppearance = newTitleTextAppearance,
                    titleTextColor = newTitleTextColor ?: deprecatedTitleTextColor ?: Color.WHITE,
                    titleText = newTitleText ?: deprecatedTitle,
                    titleTextRes = newTitleTextRes ?: if (newTitleText != null) null else deprecatedTitleResId,
                    isTitleCentered = newIsTitleCentered,
                    subtitleTextAppearance = newSubtitleTextAppearance,
                    subtitleTextColor = newSubtitleTextColor ?: deprecatedTitleTextColor ?: Color.WHITE,
                    subtitleText = newSubtitleText,
                    subtitleTextRes = newSubtitleTextRes,
                    isSubtitleCentered = newIsSubtitleCentered,
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
            val backgroundColor = widgetInfo?.colorBackground

            @ColorInt
            val colorPrimaryDark = widgetInfo?.colorPrimaryDark

            if (isIbDefaultTheme) { //if it is IB default theme apply widget color automatically to all components
                if (colorPrimary != null) {
                    style = style.copy(toolbarBackgroundColor = colorPrimary)
                }
                if (colorPrimaryDark != null) {
                    style = style.copy(statusBarBackgroundColor = colorPrimaryDark)
                }
                if (backgroundColor != null) {
                    style = style.copy(
                        titleTextColor = backgroundColor,
                        subtitleTextColor = backgroundColor,
                        navigationIconTint = backgroundColor,
                        menuItemsIconTint = backgroundColor,
                    )
                }
            } else { //if it is theme provided by integrator apply widget color only to components which are not defined by integrator
                val isBaseTheme = theme.isMMBaseTheme()
                val deprecatedColorPrimaryDefined = theme.isAttributePresent(com.google.android.material.R.attr.colorPrimary)
                val applyWidgetColorPrimary = (isBaseTheme || !deprecatedColorPrimaryDefined)

                val newBackgroundColorDefined = theme.isAttributePresent(
                        R.styleable.InAppChatToolbarViewStyleable_ibChatToolbarBackgroundColor,
                        attr,
                        R.styleable.InAppChatToolbarViewStyleable
                )
                if (applyWidgetColorPrimary && !newBackgroundColorDefined && colorPrimary != null) {
                    style = style.copy(toolbarBackgroundColor = colorPrimary)
                }

                val deprecatedColorPrimaryDarkDefined =
                        theme.isAttributePresent(androidx.appcompat.R.attr.colorPrimaryDark)
                val applyWidgetColorPrimaryDark =
                        (isBaseTheme || !deprecatedColorPrimaryDarkDefined)
                val newStatusBarBackgroundColorDefined = theme.isAttributePresent(
                        R.styleable.InAppChatToolbarViewStyleable_ibChatStatusBarBackgroundColor,
                        attr,
                        R.styleable.InAppChatToolbarViewStyleable
                )
                if (applyWidgetColorPrimaryDark && !newStatusBarBackgroundColorDefined && colorPrimaryDark != null) {
                    style = style.copy(statusBarBackgroundColor = colorPrimaryDark)
                }

                val deprecatedTitleTextColorDefined =
                        theme.isAttributePresent(androidx.appcompat.R.attr.titleTextColor)
                val applyWidgetTitleTextColor = (isBaseTheme || !deprecatedTitleTextColorDefined)

                val newTitleTextColorDefined = theme.isAttributePresent(
                        R.styleable.InAppChatToolbarViewStyleable_ibChatTitleTextColor,
                        attr,
                        R.styleable.InAppChatToolbarViewStyleable
                )
                if (applyWidgetTitleTextColor && !newTitleTextColorDefined && backgroundColor != null) {
                    style = style.copy(titleTextColor = backgroundColor)
                }
                val newSubtitleTextColorDefined = theme.isAttributePresent(
                        R.styleable.InAppChatToolbarViewStyleable_ibChatSubtitleTextColor,
                        attr,
                        R.styleable.InAppChatToolbarViewStyleable
                )
                if (applyWidgetTitleTextColor && !newSubtitleTextColorDefined && backgroundColor != null) {
                    style = style.copy(subtitleTextColor = backgroundColor)
                }
                val newNavigationIconTintDefined = theme.isAttributePresent(R.styleable.InAppChatToolbarViewStyleable_ibChatNavigationIconTint, attr, R.styleable.InAppChatToolbarViewStyleable)
                if (applyWidgetTitleTextColor && !newNavigationIconTintDefined && backgroundColor != null) {
                    style = style.copy(navigationIconTint = backgroundColor)
                }
                val newMenuItemsIconTintDefined = theme.isAttributePresent(R.styleable.InAppChatToolbarViewStyleable_ibChatMenuItemsIconTint, attr, R.styleable.InAppChatToolbarViewStyleable)
                if (applyWidgetTitleTextColor && !newMenuItemsIconTintDefined && backgroundColor != null) {
                    style = style.copy(menuItemsIconTint = backgroundColor)
                }
            }

            if (style.titleText?.isBlank() == true && widgetInfo != null) {
                style = style.copy(titleText = widgetInfo.getTitle(), titleTextRes = null)
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