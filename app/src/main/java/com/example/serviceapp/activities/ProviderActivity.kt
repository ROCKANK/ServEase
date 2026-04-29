package com.example.serviceapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.serviceapp.R
import com.example.serviceapp.domain.CategoryModel
import com.example.serviceapp.domain.ItemModel
import com.example.serviceapp.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProviderActivity : BaseActivity() {

    private val categoryList = mutableListOf<CategoryModel>()
    private val db = FirebaseDatabase.getInstance().reference

    private var serviceImageUri: Uri? = null
    private var profileImageUri: Uri? = null

    private var providerLat: Double = 0.0
    private var providerLon: Double = 0.0
    private lateinit var locationHelper: LocationHelper
    private lateinit var addBtn: Button
    private lateinit var shareLocationBtn: Button

    companion object {
        const val LOCATION_PERMISSION_REQUEST = 1002
    }


    private val pickServiceImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                serviceImageUri = uri
                findViewById<ImageView>(R.id.serviceImage).setImageURI(uri)
            }
        }

    private val pickProfileImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                profileImageUri = uri
                findViewById<ImageView>(R.id.profileImage).setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.activity_provider, null)
        setBaseView(view)

        locationHelper = LocationHelper(this)
        addBtn = findViewById(R.id.addBtn)
        shareLocationBtn = findViewById(R.id.shareLocationBtn)

        loadCategories()

        findViewById<Button>(R.id.uploadServiceBtn).setOnClickListener {
            pickServiceImage.launch("image/*")
        }

        findViewById<Button>(R.id.uploadProfileBtn).setOnClickListener {
            pickProfileImage.launch("image/*")
        }

        shareLocationBtn.setOnClickListener {
            if (!locationHelper.hasPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST
                )
            } else {
                getProviderLocation()
            }
        }

        addBtn.setOnClickListener {
            if (providerLat == 0.0 && providerLon == 0.0) {
                Toast.makeText(this, "Please share your location first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addBtn.isEnabled = false
            addBtn.text = "Uploading..."
            addService()
        }
    }

    private fun getProviderLocation() {
        shareLocationBtn.isEnabled = false
        shareLocationBtn.text = "Getting location..."

        locationHelper.getCurrentLocation(
            onSuccess = { lat, lon ->
                providerLat = lat
                providerLon = lon
                shareLocationBtn.text = "✓ Location Saved (${
                    String.format("%.4f", lat)}, ${
                    String.format("%.4f", lon)})"
                shareLocationBtn.isEnabled = true
                shareLocationBtn.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#4CAF50")
                    )
                Toast.makeText(this, "Location captured!", Toast.LENGTH_SHORT).show()
            },
            onFail = {
                shareLocationBtn.isEnabled = true
                shareLocationBtn.text = "📍 Share My Location"
                Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
            }
        )
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
            getProviderLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCategories() {
        db.child("Category").get().addOnSuccessListener {
            categoryList.clear()
            it.children.forEach { snap ->
                val cat = snap.getValue(CategoryModel::class.java)
                if (cat != null) categoryList.add(cat)
            }
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categoryList.map { it.title }
            )
            findViewById<Spinner>(R.id.categorySpinner).adapter = adapter
        }
    }

    private fun uploadImage(imageUri: Uri, onSuccess: (String) -> Unit) {
        val inputStream = contentResolver.openInputStream(imageUri)
        val bytes = inputStream!!.readBytes()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "image.jpg",
                bytes.toRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("upload_preset", "my_preset")
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/dsuzzxkhw/image/upload")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProviderActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                    resetButton()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val res = response.body?.string()
                val json = JSONObject(res!!)
                val imageUrl = json.getString("secure_url")
                runOnUiThread { onSuccess(imageUrl) }
            }
        })
    }

    private fun addService() {
        if (serviceImageUri == null) {
            Toast.makeText(this, "Select service image", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }

        uploadImage(serviceImageUri!!) { serviceUrl ->
            if (profileImageUri != null) {
                uploadImage(profileImageUri!!) { profileUrl ->
                    saveData(serviceUrl, profileUrl)
                }
            } else {
                saveData(serviceUrl, serviceUrl)
            }
        }
    }

    private fun saveData(serviceUrl: String, profileUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val serviceType = if (findViewById<RadioButton>(R.id.inHomeRadio).isChecked) "inHome" else "digital"
        val item = ItemModel(
            title = findViewById<EditText>(R.id.titleEdt).text.toString(),
            subtitle = findViewById<EditText>(R.id.subtitleEdt).text.toString(),
            description = findViewById<EditText>(R.id.descEdt).text.toString(),
            picUrl = serviceUrl,
            profilePic = profileUrl,
            price         = findViewById<EditText>(R.id.priceEdt).text.toString().toLongOrNull() ?: 0L,
            oldPrice      = findViewById<EditText>(R.id.oldPriceEdt).text.toString().toLongOrNull() ?: 0L,
            classicPrice  = findViewById<EditText>(R.id.classicPriceEdt).text.toString().toLongOrNull() ?: 0L,
            classicOldPrice = findViewById<EditText>(R.id.classicOldPriceEdt).text.toString().toLongOrNull() ?: 0L,
            premiumPrice  = findViewById<EditText>(R.id.premiumPriceEdt).text.toString().toLongOrNull() ?: 0L,
            premiumOldPrice = findViewById<EditText>(R.id.premiumOldPriceEdt).text.toString().toLongOrNull() ?: 0L,
            platinumPrice = findViewById<EditText>(R.id.platinumPriceEdt).text.toString().toLongOrNull() ?: 0L,
            platinumOldPrice = findViewById<EditText>(R.id.platinumOldPriceEdt).text.toString().toLongOrNull() ?: 0L,
            name = findViewById<EditText>(R.id.nameEdt).text.toString(),
            job = findViewById<EditText>(R.id.jobEdt).text.toString(),
            phone = findViewById<EditText>(R.id.phoneEdt).text.toString(),
            categoryId = categoryList[findViewById<Spinner>(R.id.categorySpinner).selectedItemPosition].id.toString(),
            providerId = uid,
            off = findViewById<EditText>(R.id.offTxt).text.toString().toLongOrNull() ?: 0L,
            latitude = providerLat,
            longitude = providerLon,
            serviceType = serviceType,
        )

        val key = db.child("Items").push().key!!
        item.id = key

        db.child("Items").child(key).setValue(item)
            .addOnSuccessListener {
                Toast.makeText(this, "Service Added", Toast.LENGTH_SHORT).show()
                clearForm()
                resetButton()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                resetButton()
            }
    }

    private fun clearForm() {
        listOf(
            R.id.titleEdt, R.id.subtitleEdt, R.id.descEdt,
            R.id.priceEdt, R.id.oldPriceEdt,
            R.id.classicPriceEdt, R.id.classicOldPriceEdt,
            R.id.premiumPriceEdt, R.id.premiumOldPriceEdt,
            R.id.platinumPriceEdt, R.id.platinumOldPriceEdt,
            R.id.nameEdt, R.id.jobEdt, R.id.phoneEdt, R.id.offTxt
        ).forEach { findViewById<EditText>(it).setText("") }

        findViewById<ImageView>(R.id.serviceImage)
            .setImageResource(R.drawable.ic_image_placeholder)
        findViewById<ImageView>(R.id.profileImage)
            .setImageResource(R.drawable.ic_image_placeholder)

        serviceImageUri = null
        profileImageUri = null
        providerLat = 0.0
        providerLon = 0.0

        shareLocationBtn.text = "📍 Share My Location"
        shareLocationBtn.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#6650A4")
            )
    }

    private fun resetButton() {
        addBtn.isEnabled = true
        addBtn.text = "Add Service"
    }
}