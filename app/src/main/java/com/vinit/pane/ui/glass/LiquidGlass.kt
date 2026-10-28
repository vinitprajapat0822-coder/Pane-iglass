package com.vinit.pane.ui.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs

/** User-adjustable glass intensity shared across the Compose dialer surfaces. */
val LocalLiquidGlassIntensity = compositionLocalOf { 0.72f }

/**
 * Lightweight layered glass finish. It uses translucent gradients and a specular rim rather
 * than a continuous shader, so it remains inexpensive on Android 10 / 4 GB devices.
 */
fun Modifier.liquidGlass(
    tint: Color,
    intensity: Float = 0.72f,
    shape: Shape = RoundedCornerShape(20.dp)
): Modifier {
    val strength = intensity.coerceIn(0f, 1f)
    return this
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.07f + strength * 0.15f),
                    tint.copy(alpha = 0.66f + strength * 0.28f),
                    tint.copy(alpha = 0.82f + strength * 0.16f)
                )
            ),
            shape
        )
        .border(
            width = (0.7f + strength * 0.6f).dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f + strength * 0.42f),
                    Color.White.copy(alpha = 0.05f + strength * 0.12f),
                    Color(0xFF79DCE8).copy(alpha = 0.10f + strength * 0.26f)
                )
            ),
            shape = shape
        )
    }

/** A reusable glass container for number readouts and lightweight chrome. */
@Composable
fun LiquidGlassMaterial(
    modifier: Modifier = Modifier,
    tint: Color,
    intensity: Float = LocalLiquidGlassIntensity.current,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(tint = tint, intensity = intensity, shape = shape),
        content = content
    )
}

/**
 * Floating navigation tray. A low-rate accelerometer signal shifts the ambient specular
 * highlight; compact mode lowers the surface tint and hides labels while scrolling.
 * The interaction uses gradients only (no continuous shader) for Android 10-class devices.
 */
@Composable
fun LiquidGlassTabBar(
    modifier: Modifier = Modifier,
    tint: Color,
    intensity: Float = LocalLiquidGlassIntensity.current,
    compact: Boolean = false,
    highlightFraction: Float? = null,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val ambientX = remember { mutableFloatStateOf(0.38f) }
    val ambientY = remember { mutableFloatStateOf(0.32f) }

    DisposableEffect(sensorManager) {
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || sensor == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.values.size < 2) return
                    val nextX = (0.5f + event.values[0] / (SensorManager.GRAVITY_EARTH * 2.4f))
                        .coerceIn(0.08f, 0.92f)
                    val nextY = (0.5f + event.values[1] / (SensorManager.GRAVITY_EARTH * 3.2f))
                        .coerceIn(0.12f, 0.88f)
                    if (abs(nextX - ambientX.floatValue) > 0.025f) ambientX.floatValue = nextX
                    if (abs(nextY - ambientY.floatValue) > 0.025f) ambientY.floatValue = nextY
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    val barHeight by animateDpAsState(
        targetValue = if (compact) 50.dp else 70.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f)
    )
    val resolvedIntensity = if (compact) intensity.coerceIn(0f, 1f) * 0.52f else intensity
    Row(
        modifier = modifier
            .height(barHeight)
            .liquidGlass(tint = tint, intensity = resolvedIntensity, shape = RoundedCornerShape(30.dp))
            .drawBehind {
                val focusX = (highlightFraction ?: ambientX.floatValue).coerceIn(0f, 1f)
                val alpha = if (highlightFraction != null) 0.30f else 0.08f + resolvedIntensity * 0.10f
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)),
                        center = Offset(size.width * focusX, size.height * (0.18f + ambientY.floatValue * 0.42f)),
                        radius = size.height * 1.6f
                    ),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            }
            .padding(if (compact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassTab(
    modifier: Modifier = Modifier,
    label: String,
    active: Boolean,
    icon: ImageVector,
    tint: Color,
    accent: Color,
    foreground: Color,
    muted: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    compact: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else if (active) 1.015f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f)
    )
    val shape = RoundedCornerShape(21.dp)
    val surface = if (active) {
        Modifier.liquidGlass(tint = tint, intensity = LocalLiquidGlassIntensity.current, shape = shape)
    } else {
        Modifier.clip(shape)
    }
    Column(
        modifier = modifier
            .scale(scale)
            .then(surface)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) accent else muted,
            modifier = Modifier.size(20.dp)
        )
        if (!compact) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = if (active) foreground else muted,
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

/** Press-responsive circular keypad key with spring scaling and specular edging. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassKey(
    label: String,
    modifier: Modifier = Modifier,
    tint: Color,
    foreground: Color,
    helper: String = "",
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 640f)
    )
    Box(
        modifier = modifier
            .scale(scale)
            .liquidGlass(
                tint = tint,
                intensity = if (label == "☎") 1f else
                    (LocalLiquidGlassIntensity.current + if (pressed) 0.18f else 0f).coerceAtMost(1f),
                shape = CircleShape
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = label,
                color = foreground,
                fontSize = if (label == "☎" || label == "⌫") 24.sp else 24.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 25.sp
            )
            if (helper.isNotBlank()) {
                Text(
                    text = helper,
                    color = foreground.copy(alpha = 0.60f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    lineHeight = 9.sp
                )
            }
        }
    }
}

/** Floating search glass field with an animated, one-tap clear affordance. */
@Composable
fun LiquidGlassSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    tint: Color,
    foreground: Color,
    muted: Color,
    modifier: Modifier = Modifier
) {
    LiquidGlassMaterial(
        modifier = modifier.fillMaxWidth(),
        tint = tint,
        shape = RoundedCornerShape(19.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = foreground, fontSize = 14.sp),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, color = muted, fontSize = 14.sp, maxLines = 1)
                    inner()
                }
            )
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = 0.72f, stiffness = 620f)),
                exit = fadeOut() + scaleOut(animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f))
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .clickable { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = muted, fontSize = 21.sp, lineHeight = 23.sp)
                }
            }
        }
    }
}

/** Settings control for global glass opacity and specular-edge strength. */
@Composable
fun LiquidGlassIntensityController(
    value: Float,
    onValueChange: (Float) -> Unit,
    foreground: Color,
    muted: Color,
    tint: Color,
    title: String = "Liquid Glass intensity",
    clearLabel: String = "CLEAR",
    tintedLabel: String = "TINTED",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${(value.coerceIn(0f, 1f) * 100).toInt()}%", color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF58CAD7),
                activeTrackColor = Color(0xFF58CAD7),
                inactiveTrackColor = muted.copy(alpha = 0.28f)
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(clearLabel, color = muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
            Text(tintedLabel, color = muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
        LiquidGlassMaterial(
            modifier = Modifier.fillMaxWidth(),
            tint = tint,
            intensity = value,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "Pane Liquid",
                color = foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
