package com.example.serviceapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.adapters.BookingRequestAdapter
import com.example.serviceapp.domain.BookingModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class BookingListActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTxt: TextView
    private lateinit var cardView: View

    private val bookingList = mutableListOf<BookingModel>()
    private lateinit var adapter: BookingRequestAdapter

    private var query: Query? = null
    private var listener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_booking_list, null)
        setBaseView(view)

        recyclerView = findViewById(R.id.recyclerView)
        emptyTxt = findViewById(R.id.emptyTxt)
        cardView = findViewById(R.id.cardView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BookingRequestAdapter(bookingList) { booking ->
            val intent = Intent(this, BookingRequestActivity::class.java)
            intent.putExtra("booking", booking)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadBookings()
    }

    private fun loadBookings() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        query = FirebaseDatabase.getInstance().reference
            .child("Bookings")
            .child(uid)

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bookingList.clear()
                for (snap in snapshot.children) {
                    val booking = snap.getValue(BookingModel::class.java)
                    if (booking != null) bookingList.add(booking)
                }
                adapter.notifyDataSetChanged()

                if (bookingList.isEmpty()) {
                    emptyTxt.visibility = View.VISIBLE
                    cardView.visibility = View.GONE
                } else {
                    emptyTxt.visibility = View.GONE
                    cardView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@BookingListActivity,
                    "Failed to load bookings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        query!!.addValueEventListener(listener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.let { query?.removeEventListener(it) }
    }
}