package com.example.serviceapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.serviceapp.R
import com.example.serviceapp.activities.DetailActivity
import com.example.serviceapp.databinding.ViewholderItemBinding
import com.example.serviceapp.domain.ItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ItemListCategoryAdapter(val items: MutableList<ItemModel>) :
    RecyclerView.Adapter<ItemListCategoryAdapter.Viewholder>() {

    lateinit var context: Context

    // ✅ Keep full list for restoring after search
    private val fullList = mutableListOf<ItemModel>().apply { addAll(items) }

    class Viewholder(val binding: ViewholderItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        context = parent.context
        val binding = ViewholderItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = items[position]

        holder.binding.titleTxt.text = item.title
        holder.binding.subTitletxt.text = item.subtitle.toString()

        if (!item.serviceType.isNullOrEmpty()) {
            holder.binding.typeTxt.visibility = View.VISIBLE
            holder.binding.typeTxt.text =
                if (item.serviceType == "inHome") "🏠 In-Home" else "💻 Digital"
        } else {
            holder.binding.typeTxt.visibility = View.GONE
        }

        Glide.with(context).load(item.picUrl).into(holder.binding.pic)

        val bgIndex = position % background.size
        holder.binding.mainLayout.setBackgroundResource(background[bgIndex])

        holder.itemView.setOnClickListener {
            saveRecentlyViewed(item)
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", item)
            intent.putExtra("position", bgIndex)
            context.startActivity(intent)
        }
    }

    // ✅ Call this when new data loads — updates both lists
    fun updateItems(newItems: List<ItemModel>) {
        fullList.clear()
        fullList.addAll(newItems)
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // ✅ Call this from search bar text change
    fun filter(query: String) {
        items.clear()
        if (query.isEmpty()) {
            items.addAll(fullList)
        } else {
            val lower = query.lowercase()
            items.addAll(fullList.filter { item ->
                item.title?.lowercase()?.contains(lower) == true ||
                        item.subtitle?.lowercase()?.contains(lower) == true ||
                        item.description?.lowercase()?.contains(lower) == true ||
                        item.job?.lowercase()?.contains(lower) == true
            })
        }
        notifyDataSetChanged()
    }

    private fun saveRecentlyViewed(item: ItemModel) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val key = item.id.ifEmpty {
            item.title?.replace(" ", "_")?.take(50) ?: return
        }
        item.id = key
        FirebaseDatabase.getInstance().reference
            .child("RecentlyViewed").child(uid).child(key)
            .setValue(item)
            .addOnSuccessListener {
                android.util.Log.d("RECENT", "Saved: $key")
            }
            .addOnFailureListener {
                android.util.Log.e("RECENT", "Failed to save: ${it.message}")
            }
    }

    override fun getItemCount(): Int = items.size

    private val background = listOf(
        R.drawable.pink_gradient_bg,
        R.drawable.green_gradient_bg,
        R.drawable.brown_gradient_bg,
        R.drawable.blue_gradient_bg,
    )
}