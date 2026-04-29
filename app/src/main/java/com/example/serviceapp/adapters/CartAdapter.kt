package com.example.serviceapp.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.serviceapp.R
import com.example.serviceapp.domain.ItemModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CartAdapter(
    private val list: MutableList<ItemModel>,
    private val onItemDeleted: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemPic: ShapeableImageView = itemView.findViewById(R.id.itemPic)
        val titleTxt: TextView = itemView.findViewById(R.id.titleTxt)
        val providerTxt: TextView = itemView.findViewById(R.id.providerTxt)
        val priceTxt: TextView = itemView.findViewById(R.id.priceTxt)
        val statusTxt: TextView = itemView.findViewById(R.id.statusTxt)
        val bookingIdTxt: TextView = itemView.findViewById(R.id.bookingIdTxt)
        val timeTxt: TextView = itemView.findViewById(R.id.timeTxt)
        val callBtn: AppCompatButton = itemView.findViewById(R.id.callBtn)
        val deleteBtn: AppCompatButton = itemView.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_cart_item, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = list[position]
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance().reference

        Glide.with(holder.itemView.context).load(item.picUrl).into(holder.itemPic)
        holder.titleTxt.text = item.title
        holder.providerTxt.text = "By ${item.name} • ${item.job}"
        holder.priceTxt.text = "₹${item.price}"

        val shortId = if (item.bookingId.length > 8)
            "#${item.bookingId.takeLast(8).uppercase()}"
        else if (item.bookingId.isNotEmpty()) "#${item.bookingId.uppercase()}"
        else ""
        holder.bookingIdTxt.text = shortId

        holder.timeTxt.text = if (item.timestamp > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            "🕐 ${sdf.format(Date(item.timestamp))}"
        } else ""

        val status = item.status ?: "Pending"
        updateStatusUI(holder.statusTxt, status)
        holder.statusTxt.isClickable = false
        holder.statusTxt.isFocusable = false

        holder.callBtn.setOnClickListener {
            val uri = Uri.parse("tel:${item.phone}")
            holder.itemView.context.startActivity(Intent(Intent.ACTION_DIAL, uri))
        }

        holder.deleteBtn.setOnClickListener {
            val context = holder.itemView.context
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            val currentItem = list[pos]
            val key = currentItem.id
            if (key.isNullOrEmpty()) {
                Toast.makeText(context, "Invalid item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.child("Cart").child(uid).child(key)
                .removeValue()
                .addOnSuccessListener {
                    val providerId = currentItem.providerId
                    val bookingId = currentItem.bookingId
                    if (providerId.isNotEmpty() && bookingId.isNotEmpty()) {
                        db.child("Bookings").child(providerId).child(bookingId).removeValue()
                    }
                    list.removeAt(pos)
                    notifyItemRemoved(pos)
                    if (list.isEmpty()) onItemDeleted()
                    Toast.makeText(context, "Booking removed", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateStatusUI(statusTxt: TextView, status: String) {
        statusTxt.text = status
        if (status == "Completed") {
            statusTxt.setBackgroundResource(R.drawable.status_completed_bg)
            statusTxt.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#4CAF50"))
        } else {
            statusTxt.setBackgroundResource(R.drawable.status_pending_bg)
            statusTxt.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FF9800"))
        }
    }

    override fun getItemCount() = list.size
}