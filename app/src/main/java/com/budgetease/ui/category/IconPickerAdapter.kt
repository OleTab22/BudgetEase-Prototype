package com.budgetease.ui.category

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.budgetease.R
import com.budgetease.databinding.ItemIconBinding

// Displays and manages icon selection in a grid layout
class IconPickerAdapter(
    private val context: Context,
    private val iconNames: List<String>,
    private val currentIconName: String,
    private val onIconSelected: (String) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val iconName = iconNames[position]
        holder.bind(iconName)
    }

    override fun getItemCount(): Int = iconNames.size

    // ViewHolder for displaying individual icon options
    inner class IconViewHolder(private val binding: ItemIconBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(iconName: String) {
            // Load icon resource with fallback
            val iconResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (iconResId != 0) {
                binding.iconImageView.setImageResource(iconResId)
            } else {
                // Optionally set a default/error icon if an icon name is invalid
                val fallbackIconResId = context.resources.getIdentifier("ic_default_category", "drawable", context.packageName)
                if (fallbackIconResId != 0) {
                    binding.iconImageView.setImageResource(fallbackIconResId)
                }
            }

            // Highlight the currently selected icon
            if (iconName == currentIconName) {
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_item_background))
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
            }

            binding.root.setOnClickListener {
                onIconSelected(iconName)
            }
        }
    }
} 