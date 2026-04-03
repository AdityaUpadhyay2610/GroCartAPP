package com.grocart.first.ui

import com.grocart.first.R

// This data class represents the UI state for the StartScreen
data class GroUiState(
    val clickStatus: String = "Welcome to GroCart", 
    val selectedCategory: Int = R.string.fresh_fruits // Default to a valid category ID
)