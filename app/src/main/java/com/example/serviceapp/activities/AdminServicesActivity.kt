package com.example.serviceapp.activities

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.adapters.AdminServiceAdapter
import com.example.serviceapp.domain.ItemModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminServicesActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminServiceAdapter
    private val list = ArrayList<ItemModel>()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_admin_users, null)
        setBaseView(view)

        findViewById<TextView>(R.id.titleTxt).text = "All Services"
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminServiceAdapter(list)
        recyclerView.adapter = adapter

        loadServices()
    }

    private fun loadServices() {
        db.child("Items").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                for (snap in snapshot.children) {
                    try {
                        val item = ItemModel(
                            id          = snap.key ?: "",
                            title       = snap.child("title").getValue(String::class.java),
                            name        = snap.child("name").getValue(String::class.java),
                            serviceType = snap.child("serviceType").getValue(String::class.java) ?: "inHome",
                            providerId  = snap.child("providerId").getValue(String::class.java) ?: "",
                            blocked     = snap.child("blocked").getValue(Boolean::class.java) ?: false,
                            price = snap.child("price").getValue(Long::class.java),
                            categoryId  = snap.child("categoryId").getValue(String::class.java),
                            latitude    = snap.child("latitude").getValue(Double::class.java) ?: 0.0,
                            longitude   = snap.child("longitude").getValue(Double::class.java) ?: 0.0
                        )
                        list.add(item)
                    } catch (e: Exception) {

                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}