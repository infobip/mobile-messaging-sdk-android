package org.infobip.mobile.messaging.chat.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.databinding.IbViewErrorBinding
import org.infobip.mobile.messaging.chat.utils.obtainStyledAttributes
import org.infobip.mobile.messaging.chat.utils.setImageTint
import org.infobip.mobile.messaging.chat.utils.show
import org.infobip.mobile.messaging.chat.utils.toColorStateList

class ErrorView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attributes, defStyle, defStyleRes) {

    private val binding = IbViewErrorBinding.inflate(LayoutInflater.from(context), this)

    init {
        attributes.obtainStyledAttributes(context, R.styleable.ErrorView) {
            initView(it)
        }
    }

    private fun initView(attrs: TypedArray) {
        with(binding) {
            attrs.getText(R.styleable.ErrorView_title)?.let { ibErrorViewTitle.text = it.toString() }
            attrs.getText(R.styleable.ErrorView_description)?.let { ibErrorViewDesc.text = it }
        }
    }

    fun setTitle(title: String) {
        binding.ibErrorViewTitle.text = title
    }

    fun setDescription(message: String) {
        binding.ibErrorViewDesc.text = message
    }

    fun setAction(clickAction: OnClickListener?) {
        binding.ibErrorViewActionBtn.setOnClickListener(clickAction)
    }

    fun setActionEnabled(enabled: Boolean) {
        binding.ibErrorViewActionBtn.isEnabled = enabled
    }

    fun setActionVisibility(visible: Boolean) {
        binding.ibErrorViewActionBtn.show(visible)
    }

    fun setActionIcon(@DrawableRes iconRes: Int) {
        binding.ibErrorViewActionBtn.setImageResource(iconRes)
    }

    fun setActionIconTint(@ColorInt color: Int) {
        color.toColorStateList()?.let {
            binding.ibErrorViewActionBtn.setImageTint(it)
        }
    }

    fun clear() {
        this.setTitle("")
        this.setDescription("")
        this.setAction(null)
    }

}