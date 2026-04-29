package com.example.serviceapp.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.serviceapp.domain.BannerModel
import com.example.serviceapp.domain.CategoryModel
import com.example.serviceapp.domain.ItemModel
import com.example.serviceapp.domain.ProfileModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class MainRepository {
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private val _profile = MutableLiveData<ProfileModel>()
    private val _category = MutableLiveData<List<CategoryModel>>()
    private val _banner = MutableLiveData<List<BannerModel>>()

    val profile: LiveData<ProfileModel> get() = _profile
    val category: LiveData<List<CategoryModel>> get() = _category
    val banner: LiveData<List<BannerModel>> get() = _banner

    fun loadProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = firebaseDatabase.getReference("Users").child(uid)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)

                val pic = snapshot.child("profilePic").getValue(String::class.java)
                _profile.value = ProfileModel(name = name, profilePic = pic)
            }

            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun loadCategories() {
        val ref = firebaseDatabase.getReference("Category")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CategoryModel>()
                for (child in snapshot.children) {
                    child.getValue(CategoryModel::class.java)?.let {
                        list.add(it)
                    }
                }
                _category.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun loadBanner() {
        val ref = firebaseDatabase.getReference("Banner")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BannerModel>()
                for (child in snapshot.children) {
                    child.getValue(BannerModel::class.java)?.let {
                        list.add(it)
                    }
                }
                _banner.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun loadItemCategory(categoryId: String): LiveData<MutableList<ItemModel>> {
        val itemsLiveData = MutableLiveData<MutableList<ItemModel>>()
        val ref = firebaseDatabase.getReference("Items")
        val query: Query = ref.orderByChild("categoryId").equalTo(categoryId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ItemModel>()
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ItemModel::class.java)
                    item?.let { list.add(it) }
                }
                itemsLiveData.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        return itemsLiveData
    }
}