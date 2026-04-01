

package com.grocart.first.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes // Import the material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes( // Ensure this is 'Shapes' with a capital 'S'
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp) // Example, you can have more like extraLarge
)