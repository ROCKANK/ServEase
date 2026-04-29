package com.example.serviceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.serviceapp.R
import com.example.serviceapp.databinding.ViewholderItemBinding
import com.example.serviceapp.domain.ItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ItemAdapter(
    private val list: MutableList<ItemModel>,
    private val isBookmarkScreen: Boolean = false,
    private val isProviderDashboard: Boolean = false,
    private val onEmpty: (() -> Unit)? = null
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewholderItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.binding.titleTxt.text = item.title ?: ""
        holder.binding.subTitletxt.text = item.subtitle ?: ""

        val imageUrl = if (!item.picUrl.isNullOrEmpty()) item.picUrl else item.profilePic
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(holder.binding.pic)


        holder.binding.removeBtn.visibility =
            if (isBookmarkScreen || isProviderDashboard) View.VISIBLE else View.GONE

        holder.binding.removeBtn.setOnClickListener {
            val context = holder.itemView.context
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            val currentItem = list[pos]
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = user.uid
            val key = currentItem.id
            if (key.isNullOrEmpty()) {
                Toast.makeText(context, "Invalid item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val node = if (isProviderDashboard) "Items" else "Bookmarks"

            FirebaseDatabase.getInstance().reference
                .child(node)
                .child(uid)
                .child(key)
                .removeValue()
                .addOnSuccessListener {
                    list.removeAt(pos)
                    notifyItemRemoved(pos)
                    if (list.isEmpty()) onEmpty?.invoke()
                    Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show()
                }
        }
    }
}