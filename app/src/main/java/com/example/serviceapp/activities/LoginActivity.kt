package com.example.serviceapp.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.serviceapp.R
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseDatabase.getInstance().reference
    private lateinit var googleClient: GoogleSignInClient
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEdt    = findViewById<EditText>(R.id.emailEdt)
        val passEdt     = findViewById<EditText>(R.id.passEdt)
        val loginBtn    = findViewById<Button>(R.id.loginBtn)
        val googleBtn   = findViewById<LinearLayout>(R.id.googleBtn)
        val goToRegister = findViewById<TextView>(R.id.goToRegister)
        val loadingView = findViewById<View>(R.id.loadingView)
        val loginCard   = findViewById<View>(R.id.loginCard)


        if (auth.currentUser != null) {
            loadingView.visibility = View.VISIBLE
            loginCard.visibility = View.GONE
            checkUserRoleAndNavigate()
            return
        }

        loadingView.visibility = View.GONE
        loginCard.visibility = View.VISIBLE

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        loginBtn.setOnClickListener {
            val email = emailEdt.text.toString().trim()
            val pass  = passEdt.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { checkUserRoleAndNavigate() }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        googleBtn.setOnClickListener {
            launcher.launch(googleClient.signInIntent)
        }

        goToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }


    override fun onStart() {
        super.onStart()
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                db.child("Users").child(uid).get()
                    .addOnSuccessListener {
                        if (!it.exists()) {
                            val intent = Intent(this, RegisterActivity::class.java)
                            intent.putExtra("google", true)
                            startActivity(intent)
                            finish()
                        } else {
                            checkUserRoleAndNavigate()
                        }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Google Auth Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserRoleAndNavigate() {
        if (isNavigating) return
        isNavigating = true

        val uid = auth.currentUser?.uid ?: return

        db.child("Users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val blocked = snapshot.child("blocked")
                    .getValue(Boolean::class.java) ?: false

                if (blocked) {
                    Toast.makeText(this, "Account blocked", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    isNavigating = false

                    findViewById<View>(R.id.loadingView).visibility = View.GONE
                    findViewById<View>(R.id.loginCard).visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val role = snapshot.child("role")
                    .getValue(String::class.java) ?: "user"

                val intent = when (role) {
                    "admin"    -> Intent(this, AdminDashboardActivity::class.java)
                    "provider" -> Intent(this, ProviderDashboardActivity::class.java)
                    else       -> Intent(this, MainActivity::class.java)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                isNavigating = false
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                findViewById<View>(R.id.loadingView).visibility = View.GONE
                findViewById<View>(R.id.loginCard).visibility = View.VISIBLE
            }
    }
}