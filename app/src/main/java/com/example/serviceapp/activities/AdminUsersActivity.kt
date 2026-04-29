package com.example.serviceapp.activities

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceapp.R
import com.example.serviceapp.adapters.AdminUserAdapter
import com.example.serviceapp.domain.UserModel
import com.google.firebase.database.FirebaseDatabase

class AdminUsersActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUserAdapter
    private lateinit var titleTxt: TextView
    private val list = ArrayList<UserModel>()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_admin_users, null)
        setBaseView(view)

        titleTxt = findViewById(R.id.titleTxt)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminUserAdapter(list)
        recyclerView.adapter = adapter

        val type = intent.getStringExtra("type") ?: "user"
        titleTxt.text = if (type == "provider") "All Providers" else "All Users"
        loadUsers(type)
    }

    private fun loadUsers(type: String) {
        db.child("Users").get().addOnSuccessListener { snapshot ->
            list.clear()
            for (snap in snapshot.children) {
                val user = snap.getValue(UserModel::class.java) ?: continue
                val key  = snap.key ?: continue
                val userWithId = user.copy(id = key)
                val role = userWithId.role.ifEmpty { "user" }
                if (role == type) list.add(userWithId)
            }
            adapter.notifyDataSetChanged()
        }
    }
}