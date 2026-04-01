package com.grocart.first.ui

// This data class represents the UI state for the StartScreen
data class GroUiState(
    val clickStatus: String = "Hello ViewModel", // Default message shown before any card is clicked
    val selectedCategory: Int = 0 // created a variable to store the selected category name to display
)