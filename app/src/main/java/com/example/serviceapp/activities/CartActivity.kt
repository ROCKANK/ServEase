package com.example.serviceapp.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.adapters.CartAdapter
import com.example.serviceapp.domain.ItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CartActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private val list = mutableListOf<ItemModel>()
    private lateinit var adapter: CartAdapter
    private lateinit var emptyTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_cart, null)
        setBaseView(view)

        recyclerView = findViewById(R.id.recyclerView)
        emptyTxt = findViewById(R.id.emptyTxt)

        adapter = CartAdapter(list) {
            emptyTxt.visibility = View.VISIBLE
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadCart()
    }

    private fun loadCart() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid

        FirebaseDatabase.getInstance().reference
            .child("Cart").child(uid)
            .get()
            .addOnSuccessListener {
                list.clear()
                for (snap in it.children) {
                    val item = snap.getValue(ItemModel::class.java)
                    if (item != null) list.add(item)
                }
                adapter.notifyDataSetChanged()
                emptyTxt.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load bookings", Toast.LENGTH_SHORT).show()
            }
    }
}