package com.budgetease.ui.category

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.budgetease.data.model.Category
import com.budgetease.databinding.ItemCategoryBinding

// Displays and manages category items with their icons and actions
class CategoryAdapter(
    private val context: Context, 
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category)
    }

    // ViewHolder for displaying individual category items
    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.categoryName.text = category.name
            
            // Load category icon with fallback
            val iconResId = try {
                context.resources.getIdentifier(category.icon, "drawable", context.packageName)
            } catch (e: Resources.NotFoundException) {
                context.resources.getIdentifier("ic_default_category", "drawable", context.packageName)
            }
            if (iconResId != 0) {
                binding.categoryIcon.setImageResource(iconResId)
            } else {
                binding.categoryIcon.setImageResource(context.resources.getIdentifier("ic_default_category", "drawable", context.packageName))
            }

            // Apply category color to icon
            try {
                binding.categoryIcon.colorFilter = PorterDuffColorFilter(category.color, PorterDuff.Mode.SRC_IN)
            } catch (e: Exception) {
                binding.categoryIcon.setColorFilter(ContextCompat.getColor(context, com.budgetease.R.color.primary))
            }

            // Set up action buttons
            binding.editCategoryButton.setOnClickListener {
                onEditClick(category)
            }

            binding.deleteCategoryButton.visibility = ViewGroup.VISIBLE
            binding.deleteCategoryButton.setOnClickListener {
                onDeleteClick(category)
            }
        }
    }
}

// Handles category item comparisons for efficient RecyclerView updates
class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem == newItem
    }
} 