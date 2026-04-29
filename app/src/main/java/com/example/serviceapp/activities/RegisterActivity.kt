package com.example.serviceapp.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.serviceapp.R
import com.example.serviceapp.viewModel.AuthViewModel
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import java.util.HashMap

class RegisterActivity : AppCompatActivity() {

    private lateinit var vm: AuthViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient

    private lateinit var nameEdt: EditText
    private lateinit var emailEdt: EditText
    private lateinit var phoneEdt: EditText
    private lateinit var passEdt: EditText
    private lateinit var spinner: Spinner
    private lateinit var registerBtn: Button
    private lateinit var googleBtn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        vm = ViewModelProvider(this)[AuthViewModel::class.java]
        auth = FirebaseAuth.getInstance()

        nameEdt     = findViewById(R.id.nameEdt)
        emailEdt    = findViewById(R.id.emailEdt)
        phoneEdt    = findViewById(R.id.phoneEdt)
        passEdt     = findViewById(R.id.passEdt)
        spinner     = findViewById(R.id.roleSpinner)
        registerBtn = findViewById(R.id.registerBtn)
        googleBtn   = findViewById(R.id.googleBtn)

        val roles = arrayOf("user", "provider")
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, roles)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        registerBtn.setOnClickListener {
            val name  = nameEdt.text.toString().trim()
            val email = emailEdt.text.toString().trim()
            val phone = phoneEdt.text.toString().trim()
            val pass  = passEdt.text.toString().trim()
            val role  = spinner.selectedItem.toString()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.length < 10) {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (auth.currentUser != null) {
                saveGoogleUser(role, phone)
            } else {
                if (pass.isEmpty()) {
                    Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                vm.register(name, email, pass, role, phone) {
                    if (it) navigateUser(role)
                    else Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        googleBtn.setOnClickListener {
            launcher.launch(googleClient.signInIntent)
        }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val user = auth.currentUser ?: return@addOnSuccessListener
                nameEdt.setText(user.displayName ?: "")
                emailEdt.setText(user.email ?: "")
                emailEdt.isEnabled = false
                passEdt.visibility = View.GONE
                Toast.makeText(this,
                    "Enter phone, select role and click Register",
                    Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Google Auth Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveGoogleUser(role: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return
        val name = nameEdt.text.toString().trim().ifEmpty {
            auth.currentUser?.displayName ?: ""
        }

        val map: HashMap<String, Any> = hashMapOf(
            "name"    to name,
            "email"   to emailEdt.text.toString().trim(),
            "phone"   to phone,   // ✅
            "role"    to role,
            "blocked" to false
        )

        vm.saveGoogleUser(uid, map) {
            if (it) navigateUser(role)
            else Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateUser(role: String) {
        val intent = if (role == "provider") {
            Intent(this, ProviderDashboardActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finishAffinity()
    }
}