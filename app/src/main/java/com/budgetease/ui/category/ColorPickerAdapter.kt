package com.budgetease.ui.category

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.budgetease.databinding.ItemColorChoiceBinding

// Displays and manages color selection in a grid layout
class ColorPickerAdapter(
    private val context: Context,
    private val colors: List<String>, // List of hex color strings
    private var selectedColor: String,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorChoiceBinding.inflate(LayoutInflater.from(context), parent, false)
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val colorString = colors[position]
        holder.bind(colorString)
    }

    override fun getItemCount(): Int = colors.size

    // ViewHolder for displaying individual color options
    inner class ColorViewHolder(private val binding: ItemColorChoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(colorString: String) {
            // Apply color to view background
            val color = Color.parseColor(colorString)
            (binding.colorView.background as? GradientDrawable)?.setColor(color)
                ?: binding.colorView.setBackgroundColor(color) // Fallback if not a GradientDrawable

            // Show selection indicator
            binding.selectedTick.visibility = if (colorString == selectedColor) View.VISIBLE else View.GONE

            // Handle color selection
            itemView.setOnClickListener {
                val oldSelectedPosition = colors.indexOf(selectedColor)
                selectedColor = colorString
                onColorSelected(selectedColor)
                notifyItemChanged(oldSelectedPosition)
                notifyItemChanged(adapterPosition)
            }
        }
    }
} 