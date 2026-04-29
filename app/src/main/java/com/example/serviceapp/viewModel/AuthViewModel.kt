package com.example.serviceapp.viewModel

import androidx.lifecycle.ViewModel
import com.example.serviceapp.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    fun register(
        name: String,
        email: String,
        pass: String,
        role: String,
        phone: String,
        cb: (Boolean) -> Unit
    ) {
        repo.register(name, email, pass, role, phone,cb)
    }

    fun login(
        email: String,
        pass: String,
        cb: (String?) -> Unit
    ) {
        repo.login(email, pass, cb)
    }

    fun saveGoogleUser(
        uid: String,
        map: HashMap<String, Any>,
        callback: (Boolean) -> Unit
    ) {
        repo.saveGoogleUser(uid, map, callback)
    }
}