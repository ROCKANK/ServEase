package com.example.serviceapp.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.serviceapp.R
import com.example.serviceapp.adapters.CategoryAdapter
import com.example.serviceapp.adapters.ItemListCategoryAdapter
import com.example.serviceapp.adapters.RecentlyViewedAdapter
import com.example.serviceapp.databinding.ActivityMainBinding
import com.example.serviceapp.domain.ItemModel
import com.example.serviceapp.utils.LocationHelper
import com.example.serviceapp.viewModel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class MainActivity : BaseActivity() {

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationHelper: LocationHelper

    private var userLat: Double = 0.0
    private var userLon: Double = 0.0
    private var selectedRadius: Double = 50.0
    private var isLocationFilterActive = false

    private val categoryAdapter = CategoryAdapter(mutableListOf())
    private val recentlyViewedList = mutableListOf<ItemModel>()
    private lateinit var recentlyViewedAdapter: RecentlyViewedAdapter
    private val allItems = mutableListOf<ItemModel>()

    companion object {
        const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setBaseView(binding.root, showBackButton = false)

        window.statusBarColor = ContextCompat.getColor(this, R.color.lightBrown)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        locationHelper = LocationHelper(this)

        repairUserDataIfNeeded()
        initProfile()
        initCategories()
        initBanner()
        initRecentlyViewed()
        preloadItems()
        setupSearch()
        initLocationFilter()

        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.locationBtn.setOnClickListener {
            requestLocationAndShowFilter()
        }
    }

    private fun preloadItems() {
        FirebaseDatabase.getInstance().reference
            .child("Items").get()
            .addOnSuccessListener { snapshot ->
                allItems.clear()
                for (snap in snapshot.children) {
                    try {
                        val item = snap.getValue(ItemModel::class.java) ?: continue
                        if (item.id.isEmpty()) item.id = snap.key ?: ""
                        if (!item.blocked) allItems.add(item)
                    } catch (e: Exception) { }
                }
            }
    }

    private fun setupSearch() {
        binding.searchResultsList.layoutManager = LinearLayoutManager(this)

        binding.clearSearchBtn.setOnClickListener {
            binding.editTextText.setText("")
            hideSearchResults()
        }

        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isEmpty()) {
                    hideSearchResults()
                    binding.clearSearchBtn.visibility = View.GONE
                } else {
                    binding.clearSearchBtn.visibility = View.VISIBLE
                    // ✅ Reposition overlay every keystroke
                    repositionSearchOverlay()
                    performSearch(query)
                }
            }
        })

        // ✅ Hide search when scrolling
        binding.mainScrollView.setOnTouchListener { _, _ ->
            if (binding.searchResultsCard.visibility == View.VISIBLE) {
                hideSearchResults()
                binding.editTextText.clearFocus()
            }
            false
        }
    }

    // ✅ Dynamically position overlay directly below search bar
    private fun repositionSearchOverlay() {
        binding.searchCard.post {
            val location = IntArray(2)
            binding.searchCard.getLocationOnScreen(location)
            val topOfSearchBar = location[1]
            val heightOfSearchBar = binding.searchCard.height

            val params = binding.searchResultsCard.layoutParams
                    as ConstraintLayout.LayoutParams

            params.topMargin = topOfSearchBar + heightOfSearchBar + 8
            binding.searchResultsCard.layoutParams = params
        }
    }

    private fun performSearch(query: String) {
        val lower = query.lowercase()
        val results = allItems.filter { item ->
            item.title?.lowercase()?.contains(lower) == true ||
                    item.subtitle?.lowercase()?.contains(lower) == true ||
                    item.job?.lowercase()?.contains(lower) == true ||
                    item.description?.lowercase()?.contains(lower) == true
        }.sortedByDescending { item ->
            when {
                item.title?.lowercase()?.startsWith(lower) == true -> 3
                item.title?.lowercase()?.contains(lower) == true -> 2
                else -> 1
            }
        }

        if (results.isEmpty()) {
            hideSearchResults()
        } else {
            binding.searchResultsList.adapter =
                ItemListCategoryAdapter(results.toMutableList())
            binding.searchResultsCard.visibility = View.VISIBLE
        }
    }

    private fun hideSearchResults() {
        binding.searchResultsCard.visibility = View.GONE
    }

    private fun initLocationFilter() {
        if (locationHelper.hasPermission()) {
            locationHelper.getCurrentLocation(
                onSuccess = { lat, lon ->
                    userLat = lat
                    userLon = lon
                    isLocationFilterActive = true
                    categoryAdapter.updateLocationFilter(userLat, userLon, 50.0, true)
                },
                onFail = { }
            )
        }
    }

    private fun initRecentlyViewed() {
        recentlyViewedAdapter = RecentlyViewedAdapter(recentlyViewedList)
        binding.recentlyViewedList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recentlyViewedList.adapter = recentlyViewedAdapter
        loadRecentlyViewed()
    }

    private fun loadRecentlyViewed() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("RecentlyViewed").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                recentlyViewedList.clear()
                for (snap in snapshot.children) {
                    try {
                        val item = snap.getValue(ItemModel::class.java)
                        if (item != null) {
                            if (item.id.isEmpty()) item.id = snap.key ?: ""
                            recentlyViewedList.add(0, item)
                        }
                    } catch (e: Exception) { }
                }
                recentlyViewedAdapter.notifyDataSetChanged()
                binding.recentlyViewedSection.visibility =
                    if (recentlyViewedList.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener {
                binding.recentlyViewedSection.visibility = View.GONE
            }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
        loadRecentlyViewed()
    }

    private fun repairUserDataIfNeeded() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        FirebaseDatabase.getInstance().reference
            .child("Users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val updates = mutableMapOf<String, Any>()
                val name = snapshot.child("name").getValue(String::class.java)
                if (name.isNullOrEmpty()) {
                    updates["name"] = firebaseUser?.displayName
                        ?: firebaseUser?.email?.substringBefore("@") ?: "User"
                }
                val role = snapshot.child("role").getValue(String::class.java)
                if (role.isNullOrEmpty()) updates["role"] = "user"
                if (!snapshot.hasChild("blocked")) updates["blocked"] = false
                if (updates.isNotEmpty()) {
                    FirebaseDatabase.getInstance().reference
                        .child("Users").child(uid).updateChildren(updates)
                }
            }
    }

    private fun requestLocationAndShowFilter() {
        if (!locationHelper.hasPermission()) {
            Toast.makeText(this,
                "Please grant location permission first", Toast.LENGTH_SHORT).show()
        } else {
            locationHelper.getCurrentLocation(
                onSuccess = { lat, lon ->
                    userLat = lat
                    userLon = lon
                    showRadiusDialog()
                },
                onFail = {
                    if (userLat != 0.0 && userLon != 0.0) {
                        showRadiusDialog()
                    } else {
                        Toast.makeText(this,
                            "Could not get location", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun showRadiusDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_radius, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.radiusSeekBar)
        val radiusTxt = dialogView.findViewById<TextView>(R.id.radiusTxt)

        seekBar.max = 100
        seekBar.progress = selectedRadius.toInt()
        radiusTxt.text = "${selectedRadius.toInt()} km"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val radius = if (progress < 1) 1 else progress
                radiusTxt.text = "$radius km"
                selectedRadius = radius.toDouble()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        AlertDialog.Builder(this)
            .setTitle("Show services within")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                isLocationFilterActive = true
                categoryAdapter.updateLocationFilter(userLat, userLon, selectedRadius, true)
                Toast.makeText(
                    this,
                    "Showing services within ${selectedRadius.toInt()}km",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationAndShowFilter()
        }
    }

    private fun initBanner() {
        viewModel.loadBanner()
        viewModel.banner.observe(this) { data ->
            Glide.with(this@MainActivity).load(data[0].url).into(binding.banner)
        }
    }

    private fun initProfile() {
        viewModel.loadProfile()
        viewModel.profile.observe(this) {
            binding.nameTxt.text = it.name

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            binding.greetingTxt.text = when {
                hour < 12 -> "Good morning ☀️"
                hour < 17 -> "Good afternoon 👋"
                else      -> "Good evening 🌙"
            }

            val profileClickListener = View.OnClickListener {
                startActivity(Intent(this, ProfileActivity::class.java))
            }

            if (!it.profilePic.isNullOrEmpty()) {
                binding.profilePic.visibility = View.VISIBLE
                binding.profileInitialTxt.visibility = View.GONE
                Glide.with(this@MainActivity)
                    .load(it.profilePic)
                    .skipMemoryCache(true)
                    .into(binding.profilePic)
                binding.profilePic.setOnClickListener(profileClickListener)
            } else {
                binding.profilePic.visibility = View.GONE
                binding.profileInitialTxt.visibility = View.VISIBLE
                binding.profileInitialTxt.text =
                    it.name?.firstOrNull()?.toString()?.uppercase() ?: "?"
                binding.profileInitialTxt.setOnClickListener(profileClickListener)
            }
        }
    }

    private fun initCategories() {
        binding.categoryList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoryList.adapter = categoryAdapter
        binding.progressBarCattegory.visibility = View.VISIBLE
        viewModel.loadCategories()
        viewModel.category.observe(this) {
            binding.progressBarCattegory.visibility = View.GONE
            categoryAdapter.updateData(it)
        }
    }
}