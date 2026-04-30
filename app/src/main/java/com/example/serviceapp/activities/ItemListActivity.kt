package com.example.serviceapp.activities

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serviceapp.R
import com.example.serviceapp.adapters.ItemListCategoryAdapter
import com.example.serviceapp.databinding.ActivityItemListBinding
import com.example.serviceapp.viewModel.MainViewModel

class ItemListActivity : BaseActivity() {
    private lateinit var binding: ActivityItemListBinding
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private var id: String = ""
    private var title: String = ""
    private var radiusKm: Double = 10.0
    private var userLat: Double = 0.0
    private var userLon: Double = 0.0
    private var locationFilterActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityItemListBinding.inflate(layoutInflater)
        setBaseView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.lightBrown)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        getBundles()
        initList()
    }

    private fun initList() {
        binding.apply {
            progressBar.visibility = View.VISIBLE

            viewModel.loadItems(id).observe(this@ItemListActivity) { items ->

                val filteredList = items
                    .filter { !it.blocked }
                    .filter { item ->
                        // ✅ Digital services always show — skip location filter
                        if (item.serviceType == "digital") {
                            return@filter true
                        }

                        // ✅ In-home services — apply location filter
                        if (!locationFilterActive || userLat == 0.0 || userLon == 0.0) {
                            return@filter true
                        }

                        // ✅ Items with no location set — show them
                        if (item.latitude == 0.0 && item.longitude == 0.0) {
                            return@filter true
                        }

                        val distance = calculateDistance(
                            userLat, userLon,
                            item.latitude, item.longitude
                        )
                        distance <= radiusKm
                    }

                view.layoutManager = LinearLayoutManager(
                    this@ItemListActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                view.adapter = ItemListCategoryAdapter(filteredList.toMutableList())
                progressBar.visibility = View.GONE
            }

            cartBtn.setOnClickListener {
                startActivity(Intent(this@ItemListActivity, CartActivity::class.java))
            }
        }
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000).toDouble()
    }

    private fun getBundles() {
        id = intent.getStringExtra("id")!!
        title = intent.getStringExtra("title")!!
        radiusKm = intent.getDoubleExtra("radius_km", 10.0)
        userLat = intent.getDoubleExtra("user_lat", 0.0)
        userLon = intent.getDoubleExtra("user_lon", 0.0)
        locationFilterActive = intent.getBooleanExtra("location_filter_active", false)
        binding.titleTxt.text = title
    }
}