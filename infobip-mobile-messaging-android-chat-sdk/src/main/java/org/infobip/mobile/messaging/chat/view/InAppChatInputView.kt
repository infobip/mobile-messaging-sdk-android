/*
 * InAppChatInputView.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi
import org.infobip.mobile.messaging.chat.databinding.IbViewChatInputBinding
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils
import org.infobip.mobile.messaging.chat.utils.hideKeyboard
import org.infobip.mobile.messaging.chat.utils.setImageTint
import org.infobip.mobile.messaging.chat.utils.setThrottleFirstOnClickListener
import org.infobip.mobile.messaging.chat.utils.setTint
import org.infobip.mobile.messaging.chat.utils.show
import org.infobip.mobile.messaging.chat.view.styles.InAppChatInputViewStyle
import org.infobip.mobile.messaging.chat.view.styles.factory.StyleFactory
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import java.lang.reflect.Field


class InAppChatInputView @JvmOverloads constructor(
        context: Context,
        private val attributes: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : ConstraintLayout(context, attributes, defStyle, defStyleRes) {

    companion object {
        private const val CHAT_INPUT_VISIBILITY_ANIM_DURATION_MILLIS: Long = 300
        private const val CHAT_INPUT_COUNTER_VISIBILITY_THRESHOLD: Long = 4000
    }

    private val binding = IbViewChatInputBinding.inflate(LayoutInflater.from(context), this)
    private var style = StyleFactory.create(context, attributes).chatInputViewStyle()
    private val localizationUtils = LocalizationUtils.getInstance(context)
    private var textWatcher: TextWatcher? = null

    init {
        if (style.attachmentIcon == null) {
            style = style.copy(attachmentIcon = ContextCompat.getDrawable(context, R.drawable.ib_chat_attachment_btn_icon))
        }
        if (style.sendIcon == null) {
            style = style.copy(sendIcon = ContextCompat.getDrawable(context, R.drawable.ib_chat_send_btn_icon))
        }
        applyStyle(style)
        binding.sendButton.isEnabled = getInputText()?.isNotBlank() == true
        addTextChangedListener()
    }

    fun applyWidgetInfoStyle(widgetInfo: WidgetInfo) {
        style = StyleFactory.create(context, attributes, widgetInfo).chatInputViewStyle()
        applyStyle(style)
    }

    @Suppress("DEPRECATION")
    private fun applyStyle(style: InAppChatInputViewStyle) {
        with(binding) {
            topSeparator.contentDescription = localizationUtils.getString(R.string.ib_iv_input_border_desc)
            attachmentButton.contentDescription = localizationUtils.getString(R.string.ib_iv_btn_send_attachment_desc)
            sendButton.contentDescription = localizationUtils.getString(R.string.ib_iv_btn_send_desc)
            style.textAppearance?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    messageInput.setTextAppearance(it)
                } else {
                    messageInput.setTextAppearance(context, it)
                }
            }
            messageInput.setTextColor(style.textColor)
            root.setBackgroundColor(style.backgroundColor)
            if (style.hintTextRes != null) {
                messageInput.hint = localizationUtils.getString(style.hintTextRes)
            } else if (style.hintText != null) {
                messageInput.hint = style.hintText
            }
            messageInput.setHintTextColor(style.hintTextColor)
            style.attachmentIcon?.let { attachmentButton.setImageDrawable(it) }
            style.attachmentIconTint?.let { attachmentButton.setImageTint(it) }
            style.attachmentBackgroundDrawable?.let { attachmentButton.setBackgroundDrawable(it) }
            style.attachmentBackgroundColor?.let { attachmentButton.setBackgroundColor(it) }
            style.sendIcon?.let { sendButton.setImageDrawable(it) }
            style.sendIconTint?.let { sendButton.setImageTint(it) }
            style.sendBackgroundDrawable?.let { sendButton.setBackgroundDrawable(it) }
            style.sendBackgroundColor?.let { sendButton.setBackgroundColor(it) }
            topSeparator.setBackgroundColor(style.separatorLineColor)
            topSeparator.show(style.isSeparatorLineVisible)
            messageInput.setCursorDrawableColor(style.cursorColor)
            style.charCounterTextAppearance?.let { messageInputLayout.setCounterTextAppearance(it) }
        }
        updateCharacterCounter(getInputText()?.length ?: 0)
    }

    private fun addTextChangedListener() {
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textLength = s?.length ?: 0
                val isWithinLimit = textLength <= LivechatWidgetApi.MESSAGE_MAX_LENGTH
                val isNotEmpty = s?.isNotEmpty() == true
                binding.sendButton.isEnabled = isNotEmpty && isWithinLimit
                updateCharacterCounter(textLength)
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        }.also { textWatcher = it })
    }

    private fun TextView.setCursorDrawableColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textCursorDrawable?.setTint(color)
            return
        }

        try {
            val editorField = TextView::class.java.getFieldByName("mEditor")
            val editor = editorField?.get(this) ?: this
            val editorClass: Class<*> = if (editorField != null) editor.javaClass else TextView::class.java
            val cursorRes = TextView::class.java.getFieldByName("mCursorDrawableRes")?.get(this) as? Int
                    ?: return
            val tintedCursorDrawable = ContextCompat.getDrawable(context, cursorRes)?.setTint(color = color)
                    ?: return
            val cursorField = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                editorClass.getFieldByName("mDrawableForCursor")
            } else {
                null
            }
            if (cursorField != null) {
                cursorField.set(editor, tintedCursorDrawable)
            } else {
                editorClass.getFieldByName("mCursorDrawable", "mDrawableForCursor")
                        ?.set(editor, arrayOf(tintedCursorDrawable, tintedCursorDrawable))
            }
        } catch (t: Throwable) {
            MobileMessagingLogger.e("Could not set message input cursor color.", t)
        }
    }

    private fun Class<*>.getFieldByName(vararg name: String): Field? {
        name.forEach {
            try {
                return this.getDeclaredField(it).apply { isAccessible = true }
            } catch (t: Throwable) {
            }
        }
        return null
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
        binding.sendButton.setThrottleFirstOnClickListener(listener)
    }

    fun setAttachmentButtonVisibility(visibility: Int) {
        binding.attachmentButton.visibility = visibility
    }

    fun setAttachmentButtonEnabled(isEnabled: Boolean) {
        binding.attachmentButton.isEnabled = isEnabled
    }

    fun setAttachmentButtonClickListener(listener: OnClickListener) {
        binding.attachmentButton.setThrottleFirstOnClickListener(listener)
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

    private fun updateCharacterCounter(textLength: Int) {
        val isCounterEnabled = textLength > CHAT_INPUT_COUNTER_VISIBILITY_THRESHOLD
        if (isCounterEnabled) {
            val counterColor = if (textLength > LivechatWidgetApi.MESSAGE_MAX_LENGTH) {
                style.charCounterAlertColor
            } else {
                style.charCounterDefaultColor
            }
            binding.messageInputLayout.counterTextColor = ColorStateList.valueOf(counterColor)
        }
        binding.messageInputLayout.isCounterEnabled = isCounterEnabled
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        textWatcher?.let { binding.messageInput.removeTextChangedListener(it) }
    }
}