package com.grocart.first.ui

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
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis

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
 * Uses a plain ArrayList (not a Compose state list) to avoid triggering
 * recompositions on every animation frame, which was causing scrolling jank.
 * The Canvas reads a frame-tick counter to invalidate and redraw itself without
 * rebuilding the composition tree.
 */
@Composable
fun SeasonalAnimationOverlay(groViewModel: GroViewModel, modifier: Modifier = Modifier) {
    val season = remember { getCurrentSeason() }

    val uiState by groViewModel.uiState.collectAsState()
    val categoryName = uiState.clickStatus
    val isFreezing = remember(season, categoryName) {
        season == Season.SUMMER && (categoryName.contains("Ice", ignoreCase = true) || categoryName.contains("Beverage", ignoreCase = true))
    }

    val particleCount = if (isFreezing) 40 else when (season) {
        Season.WINTER -> 30
        Season.SPRING -> 15
        Season.MONSOON -> 40
        Season.AUTUMN -> 15
        Season.SUMMER -> 20
    }

    // KEY CHANGE: Use a plain ArrayList, NOT mutableStateListOf.
    // This prevents recomposition on every frame — only the Canvas invalidates.
    val particles = remember(season, isFreezing) { ArrayList<Particle>(particleCount) }

    // Frame ticker — only invalidates the Canvas, not the whole composition tree.
    var frameTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(season, isFreezing) {
        particles.clear()
        var lastFrameTime = 0L
        while (isActive) {
            withInfiniteAnimationFrameMillis { frameTime ->
                if (lastFrameTime == 0L) {
                    lastFrameTime = frameTime
                    return@withInfiniteAnimationFrameMillis
                }

                val dt = (frameTime - lastFrameTime).coerceAtMost(50L) / 1000f
                lastFrameTime = frameTime

                for (i in particles.indices) {
                    val p = particles[i]
                    p.x += p.speedX * dt
                    p.y += p.speedY * dt
                    p.rotation += p.rotationSpeed * dt

                    if (season == Season.SUMMER && !isFreezing) {
                        if (p.y < -100f) particles[i] = createParticle(season, initialY = 3000f, randomX = true, isFreezing = isFreezing)
                    } else {
                        if (p.y > 3000f) particles[i] = createParticle(season, initialY = -50f, randomX = true, isFreezing = isFreezing)
                    }
                }

                // Bump the tick — this only invalidates the Canvas composable, not the nav host or other screens.
                frameTick = frameTime
            }
        }
    }

    // Canvas is the only composable that redraws on frameTick changes.
    // Since Canvas is a leaf node and the particle list is a plain list (not state),
    // no parent composable is ever recomposed by this animation.
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Eagerly read frameTick so Canvas re-draws each frame.
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick

        // Initialize particles on first draw
        if (particles.isEmpty() && w > 0f) {
            repeat(particleCount) {
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
            var cx = p.x
            if (p.x == -1f) { cx = Random.nextFloat() * w; p.x = cx }
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
        Season.WINTER -> {
            Particle(
                x = if (randomX) -1f else Random.nextFloat() * 1000f,
                y = initialY,
                size = 3f + Random.nextFloat() * 5f,
                speedX = -20f + Random.nextFloat() * 40f,
                speedY = 60f + Random.nextFloat() * 100f,
                rotation = 0f,
                rotationSpeed = 0f,
                alpha = 0.4f + Random.nextFloat() * 0.4f,
                color = Color.White
            )
        }
        Season.SPRING -> {
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
        Season.MONSOON -> {
            Particle(
                x = if (randomX) -1f else Random.nextFloat() * 1000f,
                y = initialY,
                size = 3f + Random.nextFloat() * 3f,
                speedX = -10f + Random.nextFloat() * 20f,
                speedY = 300f + Random.nextFloat() * 200f,
                rotation = 0f,
                rotationSpeed = 0f,
                alpha = 0.3f + Random.nextFloat() * 0.4f,
                color = Color(0xFF93C5FD)
            )
        }
        Season.AUTUMN -> {
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
                Particle(
                    x = if (randomX) -1f else Random.nextFloat() * 1000f,
                    y = if (initialY < 0f) -50f else initialY,
                    size = 2f + Random.nextFloat() * 3f,
                    speedX = -5f + Random.nextFloat() * 10f,
                    speedY = 20f + Random.nextFloat() * 40f,
                    rotation = 0f,
                    rotationSpeed = 0f,
                    alpha = 0.5f + Random.nextFloat() * 0.5f,
                    color = Color.White
                )
            } else {
                Particle(
                    x = if (randomX) -1f else Random.nextFloat() * 1000f,
                    y = if (initialY < 0f) 2500f else initialY,
                    size = 2f + Random.nextFloat() * 4f,
                    speedX = -15f + Random.nextFloat() * 30f,
                    speedY = -15f - Random.nextFloat() * 30f,
                    rotation = 0f,
                    rotationSpeed = 0f,
                    alpha = 0.2f + Random.nextFloat() * 0.3f,
                    color = Color(0xFFFDE047)
                )
            }
        }
    }
}
