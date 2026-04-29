package com.example.serviceapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.domain.BookingModel

class AdminBookingAdapter(private val list: ArrayList<BookingModel>) :
    RecyclerView.Adapter<AdminBookingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarTxt: TextView = view.findViewById(R.id.avatarTxt)
        val nameTxt: TextView = view.findViewById(R.id.nameTxt)
        val emailTxt: TextView = view.findViewById(R.id.emailTxt)
        val roleTxt: TextView = view.findViewById(R.id.roleTxt)
        val statusBtn: Button = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = list[position]

        holder.avatarTxt.text = booking.serviceTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "B"
        holder.nameTxt.text = booking.serviceTitle
        holder.emailTxt.text = "By: ${booking.userName}"
        holder.roleTxt.text = booking.serviceType

        holder.statusBtn.text = booking.status
        holder.statusBtn.isClickable = false
        holder.statusBtn.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                if (booking.status == "Completed") Color.parseColor("#4CAF50")
                else Color.parseColor("#FF9800")
            )
    }
}