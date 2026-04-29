package com.example.serviceapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.serviceapp.R
import com.example.serviceapp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.profilePic.visibility = View.VISIBLE
            binding.profileInitialTxt.visibility = View.GONE
            Glide.with(this).load(it).into(binding.profilePic)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setBaseView(binding.root)

        repairUserDataIfNeeded()
        loadUserData()

        binding.changePicBtn.setOnClickListener { pickImage.launch("image/*") }

        binding.updateBtn.setOnClickListener {
            if (selectedImageUri != null) uploadImageThenUpdate()
            else updateProfile(null)
        }

        binding.logoutBtn.setOnClickListener { logoutUser() }

        binding.helpBtn.setOnClickListener { showHelpDialog() }
    }

    private fun showHelpDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_help, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.reasonGroup)
        val titleEdt = dialogView.findViewById<EditText>(R.id.titleEdt)
        val bodyEdt = dialogView.findViewById<EditText>(R.id.bodyEdt)
        val submitBtn = dialogView.findViewById<Button>(R.id.submitBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        submitBtn.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a reason", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val reason = dialogView.findViewById<RadioButton>(selectedId).text.toString()
            val title = titleEdt.text.toString().trim()
            val body = bodyEdt.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (body.isEmpty()) {
                Toast.makeText(this, "Please describe your problem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitReport(reason, title, body)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun submitReport(reason: String, title: String, body: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseDatabase.getInstance().reference
        val reportId = db.child("Reports").push().key ?: return

        val report = mapOf(
            "reportId" to reportId,
            "userId" to user.uid,
            "userEmail" to (user.email ?: ""),
            "reason" to reason,
            "title" to title,
            "body" to body,
            "status" to "Open",
            "timestamp" to System.currentTimeMillis()
        )

        db.child("Reports").child(reportId).setValue(report)
            .addOnSuccessListener {
                Toast.makeText(this, "Report submitted! We'll get back to you.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show()
            }
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
                        .child("Users").child(uid)
                        .updateChildren(updates)
                        .addOnSuccessListener { loadUserData() }
                }
            }
    }

    private fun loadUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        FirebaseDatabase.getInstance().reference
            .child("Users").child(user.uid).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val name = it.child("name").value?.toString() ?: ""
                    val pic = it.child("profilePic").value?.toString()
                    val role = it.child("role").value?.toString() ?: "User"
                    binding.nameEdt.setText(name)
                    binding.displayNameTxt.text = name
                    binding.roleTxt.text = role.replaceFirstChar { it.uppercase() }
                    if (!pic.isNullOrEmpty()) {
                        binding.profilePic.visibility = View.VISIBLE
                        binding.profileInitialTxt.visibility = View.GONE
                        Glide.with(this).load(pic).into(binding.profilePic)
                    } else {
                        binding.profilePic.visibility = View.GONE
                        binding.profileInitialTxt.visibility = View.VISIBLE
                        binding.profileInitialTxt.text =
                            name.firstOrNull()?.toString()?.uppercase() ?: "?"
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImage(imageUri: Uri, onSuccess: (String) -> Unit) {
        val inputStream = contentResolver.openInputStream(imageUri)
        val bytes = inputStream!!.readBytes()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "image.jpg",
                bytes.toRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", "my_preset")
            .build()
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/dsuzzxkhw/image/upload")
            .post(requestBody).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                    resetButton()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string()!!)
                val imageUrl = json.getString("secure_url")
                runOnUiThread { onSuccess(imageUrl) }
            }
        })
    }

    private fun uploadImageThenUpdate() {
        binding.updateBtn.isEnabled = false
        binding.updateBtn.text = "Uploading..."
        uploadImage(selectedImageUri!!) { imageUrl -> updateProfile(imageUrl) }
    }

    private fun updateProfile(imageUrl: String?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val name = binding.nameEdt.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }
        val updates = mutableMapOf<String, Any>("name" to name)
        imageUrl?.let { updates["profilePic"] = it }
        FirebaseDatabase.getInstance().reference.child("Users").child(uid)
            .updateChildren(updates)
            .addOnSuccessListener {
                binding.displayNameTxt.text = name
                Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show()
                resetButton()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
                resetButton()
            }
    }

    private fun resetButton() {
        binding.updateBtn.isEnabled = true
        binding.updateBtn.text = "Update Profile"
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().build()
        val googleClient = GoogleSignIn.getClient(this, gso)
        googleClient.signOut().addOnCompleteListener {
            googleClient.revokeAccess()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}