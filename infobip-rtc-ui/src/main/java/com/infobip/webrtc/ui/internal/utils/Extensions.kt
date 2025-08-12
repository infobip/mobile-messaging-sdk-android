package com.infobip.webrtc.ui.internal.utils

//noinspection SuspiciousImport
import android.R
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.StyleableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale


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

/**
 * PX -> DP rounded int value
 */
internal val Int.dp: Int get() = this.dpPrecise.toInt()

/**
 * PX -> DP precise float value
 */
internal val Int.dpPrecise: Float get() = (this / Resources.getSystem().displayMetrics.density)

/**
 * DP -> PX rounded int value
 */
internal val Int.px: Int get() = this.pxPrecise.toInt()

/**
 * DP -> PX precise float value
 */
internal val Int.pxPrecise: Float get() = (this * Resources.getSystem().displayMetrics.density)

internal fun activatedColorStateList(@ColorInt activatedColor: Int, @ColorInt color: Int) = ColorStateList(
    arrayOf(
        intArrayOf(R.attr.state_activated),
        intArrayOf(-R.attr.state_activated)
    ),
    intArrayOf(
        activatedColor,
        color
    )
)

internal fun FragmentManager.navigate(destination: Fragment, @IdRes containerId: Int) {
    val tag = destination::class.java.simpleName
    if (fragments.firstOrNull()?.tag != tag) {
        commit {
            replace(containerId, destination, tag)
        }
    }
}

internal fun <T> T.applyIf(condition: T.() -> Boolean, block: T.() -> Unit): T {
    if (condition()) {
        block()
    }
    return this
}

@Suppress("DEPRECATION")
internal fun Context.applyLocale(locale: Locale): Context {
    val currentConfig = this.resources.configuration
    val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        currentConfig.locales.get(0)
    } else {
        currentConfig.locale
    }
    return if (currentLocale.language != locale.language) {
        Locale.setDefault(locale)
        val newConfig = Configuration(currentConfig)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newConfig.setLocale(locale)
        } else {
            newConfig.locale = locale
        }
        newConfig.setLayoutDirection(locale)
        this.createConfigurationContext(newConfig)
    } else {
        this
    }
}

internal fun TypedArray.resolveString(@StyleableRes index: Int, context: Context): String? {
    val nonResource = getNonResourceString(index)
    var hintText: String?
    var hintTextRes: Int?
    if (nonResource != null) {
        hintText = nonResource
    } else {
        hintText = getString(index)
        hintTextRes = getResourceId(index, 0).takeIf { it != 0 }
        if (hintTextRes != null && hintText == null) {
            hintText = context.getString(hintTextRes)
        }
    }
    return hintText
}

/**
 * Returns string value from a theme's styled attribute
 */
internal fun Context.resolveStyledStringAttribute(
    attr: Int,
    @AttrRes styleAttr: Int,
    @StyleableRes styleable: IntArray
): String? {
    val typedValue = TypedValue()
    theme.resolveAttribute(styleAttr, typedValue, true)
    return if (typedValue.data != 0) {
        val typedArray: TypedArray = obtainStyledAttributes(typedValue.data, styleable)
        typedArray.resolveString(attr, this).also { typedArray.recycle() }
    } else {
        null
    }
}

@ColorInt
internal fun Context.getColorCompat(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

internal fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> = flow {
    var windowStartTime = System.currentTimeMillis()
    var emitted = false
    collect { value ->
        val currentTime = System.currentTimeMillis()
        val delta = currentTime - windowStartTime
        if (delta >= windowDuration) {
            windowStartTime += delta / windowDuration * windowDuration
            emitted = false
        }
        if (!emitted) {
            emit(value)
            emitted = true
        }
    }
}

internal fun Activity.applyWindowInsets() {
    window?.let { window ->
        val decor = window.decorView
        WindowCompat.setDecorFitsSystemWindows(window, false)
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