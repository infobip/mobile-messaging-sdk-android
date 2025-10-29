/*
 * BottomSheetChooser.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view.chooser

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.infobip.mobile.messaging.chat.databinding.IbWidgetBottomSheetChooserBinding
import org.infobip.mobile.messaging.chat.databinding.IbWidgetBottomSheetChooserRowBinding
import org.infobip.mobile.messaging.chat.utils.show

class BottomSheetChooser<T> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val binding = IbWidgetBottomSheetChooserBinding.inflate(LayoutInflater.from(context), this)
    private var dialog: BottomSheetDialog? = null
    private lateinit var adapter: BottomSheetChooserAdapter<T>
    private var onItemSelected: ((T, BottomSheetDialog?) -> (Unit))? = null

    fun setRows(rows: List<BottomSheetRow<T>>) {
        adapter = BottomSheetChooserAdapter(rows) { row ->
            onItemSelected?.invoke(row, dialog)
        }
        binding.ibLcChooserList.adapter = adapter
    }

    fun setTitle(@StringRes title: Int) {
        binding.ibLcChooserTitle.setText(title)
        binding.ibLcChooserTitle.show(binding.ibLcChooserTitle.text.isNotBlank())
    }

    fun setTitle(title: String) {
        binding.ibLcChooserTitle.text = title
        binding.ibLcChooserTitle.show(binding.ibLcChooserTitle.text.isNotBlank())
    }

    fun setOnItemSelectedListener(onItemSelected: ((T, BottomSheetDialog?) -> (Unit))?) {
        this.onItemSelected = onItemSelected
    }

    fun createDialog(): BottomSheetDialog {
        return BottomSheetDialog(context).apply {
            setContentView(this@BottomSheetChooser)
            this.setOnShowListener {
                this.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            this@BottomSheetChooser.dialog = this
        }
    }
}

data class BottomSheetRow<T>(val text: String, val identifier: T, val smallIcon: Drawable? = null, @ColorInt val smallIconTint: Int? = null, val bigIcon: Drawable? = null)

class BottomSheetChooserAdapter<T>(
    private val data: List<BottomSheetRow<T>>,
    private val onRowClick: (T) -> Unit
) :
    RecyclerView.Adapter<BottomSheetChooserViewHolder<T>>() {

    private var selectedPosition = RecyclerView.NO_POSITION
        set(value) {
            notifyItemChanged(selectedPosition)
            field = value
            notifyItemChanged(selectedPosition)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomSheetChooserViewHolder<T> = BottomSheetChooserViewHolder(parent)

    override fun onBindViewHolder(holder: BottomSheetChooserViewHolder<T>, position: Int) {
        val row = data[position]
        val onClickListener = View.OnClickListener {
            selectedPosition = position
            onRowClick(row.identifier)
        }
        holder.bind(row, onClickListener)
    }

    override fun getItemCount(): Int = data.size
}

class BottomSheetChooserViewHolder<T>(
    parent: ViewGroup,
    private val binding: IbWidgetBottomSheetChooserRowBinding = IbWidgetBottomSheetChooserRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(row: BottomSheetRow<T>, onClick: View.OnClickListener) {
        binding.ibLcText.text = row.text
        itemView.setOnClickListener(onClick)
    }
}