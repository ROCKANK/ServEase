package com.example.serviceapp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.serviceapp.domain.BannerModel
import com.example.serviceapp.domain.CategoryModel
import com.example.serviceapp.domain.ProfileModel
import com.example.serviceapp.repository.MainRepository

class MainViewModel: ViewModel() {
    private val repository= MainRepository()

    val profile: LiveData<ProfileModel> =repository.profile
    val category: LiveData<List<CategoryModel>> = repository.category
    val banner: LiveData<List<BannerModel>> = repository.banner

    fun loadProfile() = repository.loadProfile()
    fun loadCategories() = repository.loadCategories()
    fun loadBanner() = repository.loadBanner()
    fun loadItems(categoryId: String)=repository.loadItemCategory(categoryId)
}