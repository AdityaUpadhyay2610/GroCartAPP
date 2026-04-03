package com.grocart.first.ui

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.isActive
import java.util.Calendar
import kotlin.random.Random

/** Defines the seasons mapping for dynamic themes and animations. */
enum class Season {
    SPRING, SUMMER, MONSOON, AUTUMN, WINTER
}

/** Get the current season based on device calendar. */
fun getCurrentSeason(): Season {
    val month = Calendar.getInstance().get(Calendar.MONTH) // 0-indexed (0 = Jan, 11 = Dec)
    return when (month) {
        1, 2 -> Season.SPRING      // Feb, Mar
        3, 4, 5 -> Season.SUMMER   // Apr, May, Jun
        6, 7 -> Season.MONSOON     // Jul, Aug
        8, 9, 10 -> Season.AUTUMN  // Sep, Oct, Nov
        else -> Season.WINTER      // Dec, Jan
    }
}

/** Provides a very subtle back gradient based on season, replacing static styling. */
fun getSeasonalGradient(): Brush {
    return when (getCurrentSeason()) {
        Season.SPRING -> Brush.verticalGradient(listOf(Color(0xFFFDF2F8), Color(0xFFFCE7F3))) // Soft pinks
        Season.SUMMER -> Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))) // Warm yellows
        Season.MONSOON -> Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))) // Rainy/gloomy blues
        Season.AUTUMN -> Brush.verticalGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))) // Soft orange/peach
        Season.WINTER -> Brush.verticalGradient(listOf(Color(0xFFF0FDF4), Color(0xFFE0F2FE))) // Icy blue/mint
    }
}

private data class Particle(
    var x: Float,
    var y: Float,
    var size: Float,
    var speedX: Float,
    var speedY: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var alpha: Float,
    var color: Color
)

/**
 * An overlay that draws seasonal particles across the screen.
 * Should be placed above the main content (e.g. inside FirstApp scaffolding).
 */
@Composable
fun SeasonalAnimationOverlay(groViewModel: GroViewModel, modifier: Modifier = Modifier) {
    val season = remember { getCurrentSeason() }
    val particles = remember { mutableStateListOf<Particle>() }
    
    val uiState by groViewModel.uiState.collectAsState()
    val categoryName = uiState.clickStatus
    val isFreezing = remember(season, categoryName) {
        season == Season.SUMMER && (categoryName.contains("Ice", ignoreCase = true) || categoryName.contains("Beverage", ignoreCase = true))
    }
    
    val particleCount = if (isFreezing) 40 else when (season) {
        Season.WINTER -> 30    // Moderate snowflakes
        Season.SPRING -> 15    // Some petals
        Season.MONSOON -> 40   // Heavy rain drops
        Season.AUTUMN -> 15    // Some leaves
        Season.SUMMER -> 20    // Dust/light particles
    }

    LaunchedEffect(season, isFreezing) {
        particles.clear()
        var lastFrameTime = 0L
        while (isActive) {
            withInfiniteAnimationFrameMillis { frameTime ->
                if (lastFrameTime == 0L) {
                    lastFrameTime = frameTime
                    return@withInfiniteAnimationFrameMillis
                }
                
                val dt = (frameTime - lastFrameTime) / 1000f
                lastFrameTime = frameTime

                // Update particle positions
                for (i in particles.indices) {
                    val p = particles[i]
                    p.x += p.speedX * dt
                    p.y += p.speedY * dt
                    p.rotation += p.rotationSpeed * dt

                    // Reset if out of logical bounds (Y > 3000f, or Y < -200f depending on season)
                    if (season == Season.SUMMER && !isFreezing) {
                        if (p.y < -100f) particles[i] = createParticle(season, initialY = 3000f, randomX = true, isFreezing = isFreezing)
                    } else {
                        if (p.y > 3000f) particles[i] = createParticle(season, initialY = -50f, randomX = true, isFreezing = isFreezing)
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Initialize particles smoothly on first draw
        if (particles.isEmpty() && w > 0f) {
            for (i in 0 until particleCount) {
                particles.add(createParticle(season, initialY = Random.nextFloat() * h, randomX = true, isFreezing = isFreezing))
            }
        }

        if (isFreezing) {
            val frostColor = Color.White.copy(alpha = 0.4f)
            val frostPathTop = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.12f)
                lineTo(w * 0.8f, h * 0.05f)
                lineTo(w * 0.6f, h * 0.15f)
                lineTo(w * 0.4f, h * 0.07f)
                lineTo(w * 0.2f, h * 0.13f)
                lineTo(0f, h * 0.08f)
                close()
            }
            val frostPathBottom = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h)
                lineTo(w, h)
                lineTo(w, h * 0.88f)
                lineTo(w * 0.75f, h * 0.94f)
                lineTo(w * 0.5f, h * 0.84f)
                lineTo(w * 0.25f, h * 0.95f)
                lineTo(0f, h * 0.87f)
                close()
            }
            drawPath(path = frostPathTop, color = frostColor)
            drawPath(path = frostPathBottom, color = frostColor)
        }

        particles.forEach { p ->
            // Wrap X horizontally
            var cx = p.x
            if (p.x == -1f) { cx = Random.nextFloat() * w; p.x = cx } // Setup flag handled
            else if (cx > w + 100f) p.x = -50f
            else if (cx < -100f) p.x = w + 50f
            
            withTransform({
                translate(left = p.x, top = p.y)
                rotate(degrees = p.rotation)
            }) {
                when (season) {
                    Season.WINTER -> {
                        drawCircle(color = p.color.copy(alpha = p.alpha), radius = p.size)
                    }
                    Season.SPRING -> {
                        drawOval(
                            color = p.color.copy(alpha = p.alpha),
                            topLeft = Offset(-p.size, -p.size / 2),
                            size = Size(p.size * 2, p.size)
                        )
                    }
                    Season.MONSOON -> {
                        // Thin, vertical raindrops
                        drawRect(
                            color = p.color.copy(alpha = p.alpha),
                            topLeft = Offset(-p.size / 4, -p.size * 2),
                            size = Size(p.size / 2, p.size * 4)
                        )
                    }
                    Season.AUTUMN -> {
                        drawOval(
                            color = p.color.copy(alpha = p.alpha),
                            topLeft = Offset(-p.size, -p.size / 1.5f),
                            size = Size(p.size * 2, p.size * 1.5f)
                        )
                    }
                    Season.SUMMER -> {
                        drawCircle(color = p.color.copy(alpha = p.alpha), radius = p.size)
                    }
                }
            }
        }
    }
}

private fun createParticle(season: Season, initialY: Float, randomX: Boolean = false, isFreezing: Boolean = false): Particle {
    return when (season) {
        Season.WINTER -> { // White snowflakes falling straight/slightly diagonal
            Particle(
                x = if (randomX) -1f else Random.nextFloat() * 1000f,
                y = initialY,
                size = 3f + Random.nextFloat() * 5f,
                speedX = -20f + Random.nextFloat() * 40f,
                speedY = 60f + Random.nextFloat() * 100f, // Moderate fall speed
                rotation = 0f,
                rotationSpeed = 0f,
                alpha = 0.4f + Random.nextFloat() * 0.4f,
                color = Color.White
            )
        }
        Season.SPRING -> { // Pink petals drifting
            Particle(
                x = if (randomX) -1f else Random.nextFloat() * 1000f,
                y = initialY,
                size = 6f + Random.nextFloat() * 5f,
                speedX = -40f + Random.nextFloat() * 80f,
                speedY = 40f + Random.nextFloat() * 70f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = -45f + Random.nextFloat() * 90f,
                alpha = 0.6f + Random.nextFloat() * 0.3f,
                color = if (Random.nextBoolean()) Color(0xFFF472B6) else Color(0xFFFBCFE8)
            )
        }
        Season.MONSOON -> { // Raindrops falling very fast
            Particle(
                x = if (randomX) -1f else Random.nextFloat() * 1000f,
                y = initialY,
                size = 3f + Random.nextFloat() * 3f,
                speedX = -10f + Random.nextFloat() * 20f, // Slight wind
                speedY = 300f + Random.nextFloat() * 200f, // Very fast fall
                rotation = 0f,
                rotationSpeed = 0f,
                alpha = 0.3f + Random.nextFloat() * 0.4f,
                color = Color(0xFF93C5FD)
            )
        }
        Season.AUTUMN -> { // Orange/Brown leaves falling quickly
            Particle(
                x = if (randomX) -1f else Random.nextFloat() * 1000f,
                y = initialY,
                size = 8f + Random.nextFloat() * 6f,
                speedX = 30f + Random.nextFloat() * 90f,
                speedY = 70f + Random.nextFloat() * 90f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = 30f + Random.nextFloat() * 180f,
                alpha = 0.7f + Random.nextFloat() * 0.3f,
                color = if (Random.nextBoolean()) Color(0xFFF59E0B) else Color(0xFFB45309)
            )
        }
        Season.SUMMER -> { 
            if (isFreezing) {
                // Frost sparkles falling gently downwards
                Particle(
                    x = if (randomX) -1f else Random.nextFloat() * 1000f,
                    y = if (initialY < 0f) -50f else initialY, 
                    size = 2f + Random.nextFloat() * 3f,
                    speedX = -5f + Random.nextFloat() * 10f,
                    speedY = 20f + Random.nextFloat() * 40f, // Fall down
                    rotation = 0f,
                    rotationSpeed = 0f,
                    alpha = 0.5f + Random.nextFloat() * 0.5f,
                    color = Color.White
                )
            } else {
                // Warm dust particles floating up
                Particle(
                    x = if (randomX) -1f else Random.nextFloat() * 1000f,
                    y = if (initialY < 0f) 2500f else initialY, // Spawn low so they float up
                    size = 2f + Random.nextFloat() * 4f,
                    speedX = -15f + Random.nextFloat() * 30f,
                    speedY = -15f - Random.nextFloat() * 30f, // Move UP
                    rotation = 0f,
                    rotationSpeed = 0f,
                    alpha = 0.2f + Random.nextFloat() * 0.3f,
                    color = Color(0xFFFDE047)
                )
            }
        }
    }
}
