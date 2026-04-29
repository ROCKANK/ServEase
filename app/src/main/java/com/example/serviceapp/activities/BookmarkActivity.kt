package com.example.serviceapp.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.adapters.ItemListCategoryAdapter
import com.example.serviceapp.domain.ItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.serviceapp.adapters.ItemAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener



class BookmarkActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private val list = mutableListOf<ItemModel>()
    private lateinit var adapter: ItemAdapter
    private lateinit var emptyTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = layoutInflater.inflate(R.layout.activity_bookmark, null)
        setBaseView(view)

        recyclerView = findViewById(R.id.recyclerView)
        emptyTxt = findViewById(R.id.emptyTxt)

        adapter = ItemAdapter(list, true) {
            emptyTxt.visibility = View.VISIBLE
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadBookmarks()

    }


    private fun loadBookmarks() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid

        val db = FirebaseDatabase.getInstance().reference

        db.child("Bookmarks").child(uid)
            .get()
            .addOnSuccessListener {

                list.clear()

                for (snap in it.children) {
                    try {
                        val item = snap.getValue(ItemModel::class.java)
                        if (item != null) {
                            list.add(item)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                adapter.notifyDataSetChanged()

                if (list.isEmpty()) {
                    findViewById<TextView>(R.id.emptyTxt).visibility = View.VISIBLE
                } else {
                    findViewById<TextView>(R.id.emptyTxt).visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load bookmarks", Toast.LENGTH_SHORT).show()
            }
    }
}