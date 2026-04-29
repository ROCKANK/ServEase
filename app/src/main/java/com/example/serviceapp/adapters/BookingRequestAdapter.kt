package com.example.serviceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.domain.BookingModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingRequestAdapter(
    private val list: MutableList<BookingModel>,
    private val onClick: (BookingModel) -> Unit
) : RecyclerView.Adapter<BookingRequestAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookingIdTxt: TextView = itemView.findViewById(R.id.bookingIdTxt)
        val titleTxt: TextView = itemView.findViewById(R.id.titleTxt)
        val userTxt: TextView = itemView.findViewById(R.id.userTxt)
        val userPhoneTxt: TextView = itemView.findViewById(R.id.userPhoneTxt)
        val statusTxt: TextView = itemView.findViewById(R.id.statusTxt)
        val typeTxt: TextView = itemView.findViewById(R.id.typeTxt)
        val priceTxt: TextView = itemView.findViewById(R.id.priceTxt)
        val timeTxt: TextView = itemView.findViewById(R.id.timeTxt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_booking_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = list[position]

        val shortId = if (booking.bookingId.length > 8)
            "#${booking.bookingId.takeLast(8).uppercase()}"
        else "#${booking.bookingId.uppercase()}"
        holder.bookingIdTxt.text = shortId

        holder.titleTxt.text = booking.serviceTitle
        holder.userTxt.text = "By ${booking.userName}"

        holder.userPhoneTxt.text = if (booking.userPhone.isNotEmpty())
            "📞 ${booking.userPhone}" else ""

        holder.priceTxt.text = "₹${booking.price}"
        holder.typeTxt.text = if (booking.serviceType == "digital")
            "💻 Digital" else "🏠 In-Home"

        holder.timeTxt.text = if (booking.timestamp > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            "🕐 ${sdf.format(Date(booking.timestamp))}"
        } else ""

        holder.statusTxt.text = booking.status
        holder.statusTxt.setBackgroundResource(
            if (booking.status == "Completed") R.drawable.status_completed_bg
            else R.drawable.status_pending_bg
        )

        holder.itemView.setOnClickListener { onClick(booking) }
    }

    override fun getItemCount() = list.size
}