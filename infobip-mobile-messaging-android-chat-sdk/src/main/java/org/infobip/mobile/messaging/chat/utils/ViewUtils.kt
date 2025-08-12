package org.infobip.mobile.messaging.chat.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.content.res.Resources.Theme
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R


@ColorInt
internal fun Context.getColorCompat(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

internal fun Context.getDrawableCompat(@DrawableRes id: Int): Drawable? = AppCompatResources.getDrawable(this, id)

internal fun Context.getColorStateListCompat(@ColorRes id: Int): ColorStateList = AppCompatResources.getColorStateList(this, id)

internal fun @receiver:ColorInt Int?.toColorStateList(): ColorStateList? = this?.let { ColorStateList.valueOf(it) }

internal fun ImageButton.setImageTint(color: ColorStateList) {
    this.imageTintList = color
}

internal fun ProgressBar.setProgressTint(color: ColorStateList?) {
    this.indeterminateTintList = color
}

internal fun Drawable?.setTint(color: ColorStateList?): Drawable? {
    return this?.let {
        val drawable = it.mutate()
        DrawableCompat.setTintList(drawable, color)
        drawable
    }
}

internal fun Drawable?.setTint(@ColorInt color: Int): Drawable? {
    return this?.let { drawable ->
        DrawableCompat.wrap(drawable)
            .also { DrawableCompat.setTint(it, color) }
            .let { DrawableCompat.unwrap(it) }
    }
}

/**
 * Show keyboard with view that should obtain focus
 */
internal fun View.showKeyboard() {
    val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Hide keyboard with view that is used to obtain context and windowToken
 */
internal fun View.hideKeyboard() {
    val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
}

/**
 * Set visible state of View between VISIBLE and GONE
 *
 * @property show true for VISIBLE, false for GONE
 */
internal fun View.show(show: Boolean = true) {
    this.visibility = if (show) View.VISIBLE else View.GONE
}

/**
 * Set visible state of View between GONE and VISIBLE
 *
 * @property hide true for GONE, false for VISIBLE
 */
internal fun View.hide(hide: Boolean = true) {
    show(!hide)
}

/**
 * Set visible state of View between INVISIBLE and VISIBLE
 *
 * @property invisible true for INVISIBLE, false for VISIBLE
 */
internal fun View.invisible(invisible: Boolean = true) {
    this.visibility = if (invisible) View.INVISIBLE else View.VISIBLE
}

/**
 * Set visible state of View between VISIBLE and INVISIBLE
 *
 * @property visible true for VISIBLE, false for INVISIBLE
 */
internal fun View.visible(visible: Boolean = true) {
    invisible(!visible)
}

tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    else -> (this as? ContextWrapper)?.baseContext?.activity()
}

internal fun Theme?.isIbDefaultTheme(): Boolean = isThemeAttributePresent(R.attr.ibChatDefaultStyledTheme)

internal fun Theme?.isThemeAttributePresent(attr: Int): Boolean {
    return this?.let {
        val typedValue = TypedValue()
        val isResolved = resolveAttribute(attr, typedValue, false)
        isResolved && typedValue.data != 0
    } ?: false
}

/**
 * Checks if attribute attr is present in theme as theme attribute inside theme's style attributes.
 */
internal fun Theme?.isAttributePresent(
    attr: Int,
    @AttrRes styleAttr: Int? = null,
    @StyleableRes styleable: IntArray? = null,
): Boolean {
    return this?.let {
        if (styleAttr != null && styleable != null) { //style attr
            val typedValue = TypedValue()
            resolveAttribute(styleAttr, typedValue, true)
            if (typedValue.data != 0) {
                val typedArray: TypedArray = obtainStyledAttributes(typedValue.data, styleable)
                val hasValue = typedArray.hasValue(attr)
                typedArray.recycle()
                hasValue
            } else {
                false
            }
        } else { //theme attr
            isThemeAttributePresent(attr)
        }
    } ?: false
}

/**
 * Returns receiver value if not equal 0, otherwise null. It is used if resource is defined.
 */
internal fun Int?.takeIfDefined(): Int? = this?.takeIf { it != 0 }

internal fun colorStateListOf(vararg mapping: Pair<IntArray, Int>): ColorStateList {
    val (states, colors) = mapping.unzip()
    return ColorStateList(states.toTypedArray(), colors.toIntArray())
}

@ColorInt
internal fun Context.resolveThemeColor(resId: Int): Int? = this.theme?.resolveThemeColor(resId)

@ColorInt
internal fun Theme.resolveThemeColor(resId: Int): Int? {
    return TypedValue().run {
        this@resolveThemeColor.resolveAttribute(resId, this, true)
        this.data.takeIfDefined()
    }
}

@get:ColorInt
internal val WidgetInfo.colorPrimary: Int?
    get() = runCatching { this.primaryColor.toColorInt() }.getOrNull()

@get:ColorInt
internal val WidgetInfo.colorBackground: Int?
    get() = runCatching { this.backgroundColor.toColorInt() }.getOrNull()

@get:ColorInt
internal val WidgetInfo.colorPrimaryText: Int?
    get() = runCatching { this.primaryTextColor.toColorInt() }.getOrNull()

@get:ColorInt
internal val WidgetInfo.colorPrimaryDark: Int?
    get() = colorPrimary?.let { ColorUtils.blendARGB(it, Color.BLACK, 0.2f) }

internal fun Activity.applyWindowInsets() {
    window?.let { window ->
        val decor = window.decorView
        ViewGroupCompat.installCompatInsetsDispatch(decor)
        ViewCompat.setOnApplyWindowInsetsListener(decor) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
        decor.post { ViewCompat.requestApplyInsets(decor) }
    }
}

internal fun Activity?.setStatusBarColor(@ColorInt color: Int?) {
    if (color != null) {
        this?.window?.decorView?.rootView?.setBackgroundColor(color)
    }
}

internal fun ComponentActivity?.setSystemBarIconsColor(lightIcons: Boolean) {
    val systemBarStyle = if (lightIcons) {
        SystemBarStyle.dark(Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    }
    this?.enableEdgeToEdge(
        statusBarStyle = systemBarStyle,
        navigationBarStyle = systemBarStyle
    )
}

internal fun TypedArray.resolveStringWithResId(
    context: Context,
    @StyleableRes stringAttr: Int,
    defValue: Int? = null,
): Pair<Int?, String?> {
    var value: String? = getString(stringAttr)
    var resource: Int? = getResourceId(stringAttr, 0).takeIfDefined()
    if (resource == null && value == null && defValue != null && defValue > 0) {
        resource = defValue
        value = context.getString(resource)
    }
    return Pair(resource, value)
}