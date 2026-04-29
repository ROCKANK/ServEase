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
import com.example.serviceapp.databinding.ViewholderRecentBinding
import com.example.serviceapp.domain.ItemModel

class RecentlyViewedAdapter(val items: MutableList<ItemModel>) :
    RecyclerView.Adapter<RecentlyViewedAdapter.Viewholder>() {

    lateinit var context: Context

    class Viewholder(val binding: ViewholderRecentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        context = parent.context
        val binding = ViewholderRecentBinding.inflate(
            LayoutInflater.from(context), parent, false
        )
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = items[position]

        holder.binding.titleTxt.text = item.title
        holder.binding.priceTxt.text = "₹${item.price}"
        holder.binding.typeTxt.text =
            if (item.serviceType == "inHome") "🏠 In-Home" else "💻 Digital"

        Glide.with(context).load(item.picUrl).into(holder.binding.pic)

        holder.itemView.setOnClickListener {
            val bgIndex = position % bgList.size
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", item)
            intent.putExtra("position", bgIndex)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    private val bgList = listOf(
        R.drawable.pink_gradient_bg,
        R.drawable.green_gradient_bg,
        R.drawable.brown_gradient_bg,
        R.drawable.blue_gradient_bg,
    )
}