package org.infobip.mobile.messaging.chat.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Build
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.databinding.IbViewChatInputBinding
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils
import org.infobip.mobile.messaging.chat.utils.hideKeyboard
import org.infobip.mobile.messaging.chat.utils.setImageTint
import org.infobip.mobile.messaging.chat.utils.show
import org.infobip.mobile.messaging.chat.view.styles.InAppChatInputViewStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatInputViewStyle.Companion.applyWidgetConfig

class InAppChatInputView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attributes, defStyle, defStyleRes) {

    companion object {
        private const val CHAT_INPUT_VISIBILITY_ANIM_DURATION_MILLIS: Long = 300
    }

    private val binding = IbViewChatInputBinding.inflate(LayoutInflater.from(context), this)
    var style = InAppChatInputViewStyle(context, attributes)

    init {
        applyStyle(style)
        binding.sendButton.isEnabled = getInputText()?.isNotBlank() == true
    }

    fun applyWidgetInfoStyle(widgetInfo: WidgetInfo) {
        style = style.applyWidgetConfig(context, widgetInfo)
        applyStyle(style)
    }

    private fun applyStyle(style: InAppChatInputViewStyle) {
        with(binding) {
            style.textAppearance?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    messageInput.setTextAppearance(it)
                } else {
                    messageInput.setTextAppearance(context, it)
                }
            }
            root.setBackgroundColor(style.backgroundColor)
            if (style.hintText != null) {
                messageInput.hint = style.hintText
            } else {
                style.hintTextRes?.let { messageInput.hint = context.getString(it) }
            }
            messageInput.setHintTextColor(style.hintTextColor)
            style.attachmentIcon?.let { attachmentButton.setImageResource(it) }
            style.attachmentIconTint?.let { attachmentButton.setImageTint(it) }
            style.sendIcon?.let { sendButton.setImageResource(it) }
            style.sendIconTint?.let { sendButton.setImageTint(it) }
            topSeparator.setBackgroundColor(style.separatorLineColor)
            topSeparator.show(style.isSeparatorLineVisible)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        this.children.forEach {
            it.isEnabled = enabled
        }
    }

    fun setSendButtonEnabled(isEnabled: Boolean) {
        binding.sendButton.isEnabled = isEnabled
    }

    fun setSendButtonClickListener(listener: OnClickListener) {
        binding.sendButton.setOnClickListener(listener)
    }

    fun setAttachmentButtonEnabled(isEnabled: Boolean) {
        binding.attachmentButton.isEnabled = isEnabled
    }

    fun setAttachmentButtonClickListener(listener: OnClickListener) {
        binding.attachmentButton.setOnClickListener(listener)
    }

    fun getInputText(): String? = binding.messageInput.text?.toString()

    fun clearInputText() = binding.messageInput.text?.clear()

    fun addInputTextChangeListener(textWatcher: TextWatcher) {
        binding.messageInput.addTextChangedListener(textWatcher)
    }

    fun removeInputTextChangeListener(textWatcher: TextWatcher) {
        binding.messageInput.removeTextChangedListener(textWatcher)
    }

    fun setInputFocusChangeListener(listener: OnFocusChangeListener) {
        binding.messageInput.onFocusChangeListener = listener
    }

    fun refreshLocalisation(localizationUtils: LocalizationUtils) = with(binding) {
        topSeparator.contentDescription = localizationUtils.getString(R.string.ib_iv_input_border_desc)
        attachmentButton.contentDescription = localizationUtils.getString(R.string.ib_iv_btn_send_attachment_desc)
        sendButton.contentDescription = localizationUtils.getString(R.string.ib_iv_btn_send_desc)
        style.hintTextRes?.let { messageInput.hint = localizationUtils.getString(it) }
    }

    fun show(show: Boolean) {
        if (show) {
            animate().translationY(0f)
                .setDuration(CHAT_INPUT_VISIBILITY_ANIM_DURATION_MILLIS)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        visibility = VISIBLE
                    }
                })
        } else {
            animate().translationY(this.height.toFloat())
                .setDuration(CHAT_INPUT_VISIBILITY_ANIM_DURATION_MILLIS)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        visibility = GONE
                    }
                })
        }
    }

    fun hideKeyboard() {
        binding.messageInput.hideKeyboard()
    }

}