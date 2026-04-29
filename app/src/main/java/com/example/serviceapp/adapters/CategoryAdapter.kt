package com.example.serviceapp.adapters

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.activities.ItemListActivity
import com.example.serviceapp.databinding.ViewholderCategoryBinding
import com.example.serviceapp.domain.CategoryModel

class CategoryAdapter(
    private val items: MutableList<CategoryModel>,
    private var userLat: Double = 0.0,
    private var userLon: Double = 0.0,
    private var radiusKm: Double = 10.0,
    private var locationFilterActive: Boolean = false
) : RecyclerView.Adapter<CategoryAdapter.Viewholder>() {

    private lateinit var context: Context
    private var selectedPosition = -1
    private var lastSelectedPosition = -1

    inner class Viewholder(val binding: ViewholderCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        context = parent.context
        val binding = ViewholderCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = items[position]
        holder.binding.titleTxt.text = item.title

        holder.binding.root.setOnClickListener {
            if (selectedPosition != position) {
                lastSelectedPosition = selectedPosition
                selectedPosition = position
                if (lastSelectedPosition != -1) notifyItemChanged(lastSelectedPosition)
                notifyItemChanged(selectedPosition)
            }

            android.os.Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(context, ItemListActivity::class.java).apply {
                    putExtra("id", item.id.toString())
                    putExtra("title", item.title)
                    putExtra("user_lat", userLat)
                    putExtra("user_lon", userLon)
                    putExtra("radius_km", radiusKm)
                    putExtra("location_filter_active", locationFilterActive)
                }
                ContextCompat.startActivity(context, intent, null)
            }, 500)
        }

        val isSelected = selectedPosition == position
        holder.binding.cat.setBackgroundResource(
            if (isSelected) R.drawable.black_bg else R.drawable.purple_bg
        )
        holder.binding.titleTxt.setTextColor(
            if (isSelected) holder.itemView.context.resources.getColor(R.color.white)
            else holder.itemView.context.resources.getColor(R.color.white)
        )
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newData: List<CategoryModel>) {
        items.clear()
        items.addAll(newData)
        notifyDataSetChanged()
    }

    fun updateLocationFilter(
        lat: Double,
        lon: Double,
        radius: Double,
        isActive: Boolean
    ) {
        userLat = lat
        userLon = lon
        radiusKm = radius
        locationFilterActive = isActive
    }
}