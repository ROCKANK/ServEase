package com.example.serviceapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.domain.UserModel
import com.google.firebase.database.FirebaseDatabase

class AdminUserAdapter(private val list: ArrayList<UserModel>) :
    RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarTxt: TextView = view.findViewById(R.id.avatarTxt)
        val nameTxt: TextView   = view.findViewById(R.id.nameTxt)
        val emailTxt: TextView  = view.findViewById(R.id.emailTxt)
        val roleTxt: TextView   = view.findViewById(R.id.roleTxt)
        val blockBtn: Button    = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user   = list[position]
        val userId = user.id

        holder.avatarTxt.text = user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        holder.nameTxt.text   = user.name.ifEmpty { "Unknown" }
        holder.emailTxt.text  = user.email
        holder.roleTxt.text   = user.role

        updateBlockButton(holder.blockBtn, user.blocked)

        holder.blockBtn.setOnClickListener {
            val newBlocked = !user.blocked
            FirebaseDatabase.getInstance().reference
                .child("Users").child(userId)
                .child("blocked").setValue(newBlocked)
                .addOnSuccessListener {
                    val currentPos = list.indexOfFirst { it.id == userId }
                    if (currentPos != -1) {
                        list[currentPos] = list[currentPos].copy(blocked = newBlocked)
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