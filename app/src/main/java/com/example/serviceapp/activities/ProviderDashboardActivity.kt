package com.example.serviceapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.adapters.ProviderAdapter
import com.example.serviceapp.domain.ItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class ProviderDashboardActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTxt: TextView
    private lateinit var cardView: View
    private lateinit var bookingRequestsBtn: ImageView

    private val serviceList = mutableListOf<ItemModel>()
    private lateinit var providerAdapter: ProviderAdapter

    private var dbRef: DatabaseReference? = null
    private var query: Query? = null
    private var listener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = layoutInflater.inflate(R.layout.activity_provider_dashboard, null)
        setBaseView(view)

        recyclerView = findViewById(R.id.recyclerView)
        emptyTxt = findViewById(R.id.emptyTxt)
        cardView = findViewById(R.id.cardView)
        bookingRequestsBtn = findViewById(R.id.bookingRequestsBtn)

        recyclerView.layoutManager = LinearLayoutManager(this)

        providerAdapter = ProviderAdapter(serviceList)
        recyclerView.adapter = providerAdapter

        bookingRequestsBtn.setOnClickListener {
            startActivity(Intent(this, BookingListActivity::class.java))
        }

        loadMyServices()
    }

    private fun loadMyServices() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        dbRef = FirebaseDatabase.getInstance().reference.child("Items")
        query = dbRef!!.orderByChild("providerId").equalTo(currentUid)

        listener?.let { query?.removeEventListener(it) }

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                serviceList.clear()
                for (snap in snapshot.children) {
                    val item = snap.getValue(ItemModel::class.java)
                    if (item != null) {
                        item.id = snap.key ?: ""
                        serviceList.add(item)
                    }
                }
                providerAdapter.notifyDataSetChanged()

                if (serviceList.isEmpty()) {
                    emptyTxt.visibility = View.VISIBLE
                    cardView.visibility = View.GONE
                } else {
                    emptyTxt.visibility = View.GONE
                    cardView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ProviderDashboardActivity,
                    "Failed to load services",
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