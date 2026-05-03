package com.example.serviceapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.serviceapp.R
import com.example.serviceapp.domain.ReviewModel
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewAdapter(private val list: List<ReviewModel>) :
    RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reviewerPic: ShapeableImageView = itemView.findViewById(R.id.reviewerPic)
        val reviewerInitialTxt: TextView = itemView.findViewById(R.id.reviewerInitialTxt)
        val reviewerNameTxt: TextView = itemView.findViewById(R.id.reviewerNameTxt)
        val reviewDateTxt: TextView = itemView.findViewById(R.id.reviewDateTxt)
        val reviewRatingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)
        val reviewBodyTxt: TextView = itemView.findViewById(R.id.reviewBodyTxt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = list[position]

        holder.reviewerNameTxt.text = review.userName
        holder.reviewRatingBar.rating = review.rating
        holder.reviewBodyTxt.text = review.body

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.reviewDateTxt.text = sdf.format(Date(review.timestamp))

        if (review.userPic.isNotEmpty()) {
            holder.reviewerPic.visibility = View.VISIBLE
            holder.reviewerInitialTxt.visibility = View.GONE
            Glide.with(holder.itemView.context).load(review.userPic).into(holder.reviewerPic)
        } else {
            holder.reviewerPic.visibility = View.GONE
            holder.reviewerInitialTxt.visibility = View.VISIBLE
            holder.reviewerInitialTxt.text =
                review.userName.firstOrNull()?.toString()?.uppercase() ?: "?"
        }
    }

    override fun getItemCount() = list.size
}