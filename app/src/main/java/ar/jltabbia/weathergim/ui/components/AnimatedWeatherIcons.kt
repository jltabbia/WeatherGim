package ar.jltabbia.weathergim.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ar.jltabbia.weathergim.data.model.WeatherCondition

@Composable
fun AnimatedWeatherIcon(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (condition) {
            WeatherCondition.SUNNY -> AnimatedSunny(size)
            WeatherCondition.PARTLY_CLOUDY -> AnimatedPartlyCloudy(size)
            WeatherCondition.CLOUDY -> AnimatedCloudy(size)
            WeatherCondition.RAINY -> AnimatedRainy(size)
            WeatherCondition.THUNDERSTORM -> AnimatedThunderstorm(size)
            WeatherCondition.SNOWY -> AnimatedSnowy(size)
            WeatherCondition.WINDY -> AnimatedWindy(size)
        }
    }
}

@Composable
fun AnimatedSunny(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "sunny")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        val radius = size.toPx() * 0.28f * scale

        // Sun core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD54F), Color(0xFFFF8F00)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Rotating rays
        rotate(rotation, pivot = center) {
            val rayLength = size.toPx() * 0.18f
            val rayWidth = size.toPx() * 0.04f
            for (i in 0 until 8) {
                val angle = i * 45f
                rotate(angle, pivot = center) {
                    drawRoundRect(
                        color = Color(0xFFFFCA28),
                        topLeft = Offset(center.x - rayWidth / 2, center.y - radius - rayLength - 4f),
                        size = androidx.compose.ui.geometry.Size(rayWidth, rayLength),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedPartlyCloudy(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "partly_cloudy")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Background small sun
        drawCircle(
            color = Color(0xFFFFB300),
            radius = w * 0.22f,
            center = Offset(w * 0.65f + offsetX * 0.5f, h * 0.35f)
        )

        // Cloud foreground
        drawCloud(Offset(w * 0.35f + offsetX, h * 0.55f), w * 0.5f)
    }
}

@Composable
fun AnimatedCloudy(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "cloudy")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        drawCloud(Offset(w * 0.5f + offsetX, h * 0.5f), w * 0.58f, Color(0xFFCFD8DC))
        drawCloud(Offset(w * 0.4f - offsetX * 0.5f, h * 0.4f), w * 0.45f, Color(0xFFECEFF1))
    }
}

@Composable
fun AnimatedRainy(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainy")
    val dropAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drop"
    )

    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Cloud
        drawCloud(Offset(w * 0.5f, h * 0.4f), w * 0.55f, Color(0xFF78909C))

        // Rain drops
        val dropColor = Color(0xFF4FC3F7)
        val startY = h * 0.65f + dropAnim
        for (i in 0..3) {
            val startX = w * (0.3f + i * 0.15f)
            val currentY = (startY + (i * 3f)) % h
            drawLine(
                color = dropColor,
                start = Offset(startX, currentY),
                end = Offset(startX - 2f, currentY + 8f),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun AnimatedThunderstorm(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "thunder")
    val alphaFlash by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash"
    )

    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        drawCloud(Offset(w * 0.5f, h * 0.38f), w * 0.55f, Color(0xFF37474F))

        // Lightning bolt
        val lightningPath = Path().apply {
            moveTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.52f, h * 0.72f)
            lineTo(w * 0.45f, h * 0.92f)
            lineTo(w * 0.62f, h * 0.68f)
            lineTo(w * 0.52f, h * 0.68f)
            close()
        }
        drawPath(lightningPath, color = Color(0xFFFFEE58).copy(alpha = alphaFlash))
    }
}

@Composable
fun AnimatedSnowy(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val snowAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow"
    )

    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        drawCloud(Offset(w * 0.5f, h * 0.4f), w * 0.55f, Color(0xFFB0BEC5))

        val startY = h * 0.65f + snowAnim
        for (i in 0..3) {
            val startX = w * (0.3f + i * 0.15f)
            val currentY = (startY + (i * 2f)) % h
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(startX, currentY))
        }
    }
}

@Composable
fun AnimatedWindy(size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "wind")
    val windOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wind"
    )

    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        val windColor = Color(0xFF90CAF9)
        drawArc(
            color = windColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.2f + windOffset, h * 0.3f),
            size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
        drawArc(
            color = windColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.1f - windOffset, h * 0.6f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
    }
}

// Helper to draw a fluffy cloud
private fun DrawScope.drawCloud(
    center: Offset,
    width: Float,
    color: Color = Color(0xFFE0E0E0)
) {
    val r = width * 0.3f
    drawCircle(color, radius = r * 0.8f, center = Offset(center.x - r * 0.7f, center.y))
    drawCircle(color, radius = r * 1.1f, center = Offset(center.x, center.y - r * 0.3f))
    drawCircle(color, radius = r * 0.9f, center = Offset(center.x + r * 0.8f, center.y))
    drawRect(
        color = color,
        topLeft = Offset(center.x - r * 0.9f, center.y - r * 0.1f),
        size = androidx.compose.ui.geometry.Size(r * 1.8f, r * 0.9f)
    )
}
