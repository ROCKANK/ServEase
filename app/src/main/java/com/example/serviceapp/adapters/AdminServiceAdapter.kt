package com.example.serviceapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.domain.ItemModel
import com.google.firebase.database.FirebaseDatabase

class AdminServiceAdapter(private val list: ArrayList<ItemModel>) :
    RecyclerView.Adapter<AdminServiceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarTxt: TextView = view.findViewById(R.id.avatarTxt)
        val nameTxt: TextView = view.findViewById(R.id.nameTxt)
        val emailTxt: TextView = view.findViewById(R.id.emailTxt)
        val roleTxt: TextView = view.findViewById(R.id.roleTxt)
        val blockBtn: Button = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val itemId = item.id

        holder.avatarTxt.text = item.title?.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
        holder.nameTxt.text = item.title ?: "Untitled"
        holder.emailTxt.text = "By: ${item.name ?: "Unknown provider"}"
        holder.roleTxt.text = item.serviceType

        updateBlockButton(holder.blockBtn, item.blocked)

        holder.blockBtn.setOnClickListener {
            val newBlocked = !item.blocked
            FirebaseDatabase.getInstance().reference
                .child("Items").child(itemId)
                .child("blocked").setValue(newBlocked)
                .addOnSuccessListener {
                    val currentPos = list.indexOfFirst { it.id == itemId }
                    if (currentPos != -1) {
                        list[currentPos].blocked = newBlocked
                        notifyItemChanged(currentPos)
                    }
                }
        }
    }

    private fun updateBlockButton(btn: Button, isBlocked: Boolean) {
        if (isBlocked) {
            btn.text = "Unblock"
            btn.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        } else {
            btn.text = "Block"
            btn.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#6650A4"))
        }
    }
}