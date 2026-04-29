package com.example.serviceapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.databinding.ItemProviderDashboardBinding
import com.example.serviceapp.domain.ItemModel
import com.google.firebase.database.FirebaseDatabase

class ProviderAdapter(private val list: MutableList<ItemModel>) :
    RecyclerView.Adapter<ProviderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProviderDashboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProviderDashboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.titleTxt.text = item.title ?: ""

        val key = item.id

        holder.binding.deleteBtn.setOnClickListener {
            val context = holder.itemView.context

            if (key.isEmpty()) {
                Toast.makeText(context, "Invalid item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseDatabase.getInstance().reference
                .child("Items")
                .child(key)
                .removeValue()
                .addOnSuccessListener {

                    val currentPos = list.indexOfFirst { it.id == key }
                    if (currentPos != -1) {
                        list.removeAt(currentPos)
                        notifyItemRemoved(currentPos)
                        notifyItemRangeChanged(currentPos, list.size)
                    }
                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Delete failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}