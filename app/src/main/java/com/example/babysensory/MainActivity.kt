package com.example.babysensory

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.min
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        // Android generally exposes the current system wallpaper to ordinary apps.
        // Separate lock-screen wallpaper access is device/OS dependent.
        val wallpaper = runCatching {
            val drawable = WallpaperManager.getInstance(this).drawable ?: return@runCatching null
            val w = drawable.intrinsicWidth.coerceIn(1, 1600)
            val h = drawable.intrinsicHeight.coerceIn(1, 3000)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp
        }.getOrNull()

        setContent { SensoryApp(wallpaper) }
    }
}

private data class SensorPoint(val x: Float, val y: Float, val hue: Float)

@Composable
private fun SensoryApp(wallpaper: Bitmap?) {
    var count by remember { mutableIntStateOf(6) }
    var showSetup by remember { mutableStateOf(true) }
    var layoutSeed by remember { mutableIntStateOf(0) }

    val points = remember(count, layoutSeed) {
        val random = Random(layoutSeed + count * 31)
        // Keep targets away from screen edges and spread them across the display.
        List(count) { i ->
            val columns = when {
                count <= 3 -> 2
                count <= 8 -> 3
                else -> 4
            }
            val rows = (count + columns - 1) / columns
            val col = i % columns
            val row = i / columns
            val x = (col + 0.5f) / columns + random.nextFloat() * 0.08f - 0.04f
            val y = (row + 0.5f) / rows + random.nextFloat() * 0.07f - 0.035f
            SensorPoint(x.coerceIn(0.12f, 0.88f), y.coerceIn(0.12f, 0.88f),
                listOf(205f, 190f, 225f, 265f, 175f, 315f)[i % 6])
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            // Darken the wallpaper so the sensor illumination remains readable.
            Box(Modifier.fillMaxSize().background(Color(0xA9000000)))
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF05070D), Color(0xFF080B16), Color(0xFF03040A))
                    )
                )
            )
        }

        points.forEachIndexed { index, point ->
            FingerprintTarget(point, index)
        }

        Box(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp)) {
            TextButton(onClick = { showSetup = true }) {
                Text("SETUP", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }

        if (showSetup) {
            SetupDialog(count) {
                count = it
                layoutSeed++
                showSetup = false
            }
        }
    }
    BackHandler(enabled = !showSetup) { showSetup = true }
}

@Composable
private fun FingerprintTarget(point: SensorPoint, index: Int) {
    var pressed by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "sensor-$index")
    val idlePulse by transition.animateFloat(
        initialValue = 0.96f, targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse), label = "idle"
    )
    val touchPulse by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing),
            RepeatMode.Reverse), label = "touch"
    )
    val haptics = LocalHapticFeedback.current
    val sensorColor = Color.hsv(point.hue, 0.72f, 1f)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val targetSize = (min(maxWidth.value, maxHeight.value) * 0.19f).coerceIn(76f, 116f)
        Box(
            Modifier
                .offset(
                    x = maxWidth * point.x - targetSize.dp / 2,
                    y = maxHeight * point.y - targetSize.dp / 2
                )
                .size(targetSize.dp)
                .clip(RoundedCornerShape(50))
                .pointerInput(index) {
                    detectTapGestures(onPress = {
                        pressed = true
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        pressed = false
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val scale = if (pressed) touchPulse else idlePulse
                val cx = size.width / 2f
                val cy = size.height / 2f
                val base = size.minDimension * 0.34f * scale

                // Subtle optical-sensor halo, like an illuminated under-display reader.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            sensorColor.copy(alpha = if (pressed) 0.34f else 0.12f),
                            sensorColor.copy(alpha = 0.055f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(cx, cy),
                        radius = base * 2.7f
                    ),
                    radius = base * 2.7f,
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )

                // Fine outer ring and bright inner ring.
                drawCircle(sensorColor.copy(alpha = if (pressed) 0.82f else 0.52f),
                    radius = base * 1.55f, style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (pressed) 2.2.dp.toPx() else 1.35.dp.toPx()))
                drawCircle(sensorColor.copy(alpha = if (pressed) 0.95f else 0.72f),
                    radius = base, style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (pressed) 3.0.dp.toPx() else 2.0.dp.toPx()))
                drawCircle(sensorColor.copy(alpha = if (pressed) 0.10f else 0.035f),
                    radius = base * 0.88f)

                if (pressed) {
                    // Three soft expanding optical rings while a finger is held down.
                    for (ring in 0..2) {
                        val r = base * (1.18f + ring * 0.43f) * touchPulse
                        drawCircle(
                            color = sensorColor.copy(alpha = 0.30f - ring * 0.075f),
                            radius = r,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupDialog(initial: Int, onStart: (Int) -> Unit) {
    var value by remember { mutableStateOf(initial.toString()) }
    val number = value.toIntOrNull()
    val valid = number != null && number in 1..24

    Dialog(onDismissRequest = { onStart(initial) }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Color(0xFF111522),
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sensory Playground", color = Color.White, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Choose how many fingerprint lights to show",
                    color = Color(0xFFB7C0D5), fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(2) },
                    label = { Text("Number of sensors") },
                    singleLine = true,
                    isError = value.isNotEmpty() && !valid
                )
                Spacer(Modifier.height(8.dp))
                Text("1–24 sensors • silent • gentle touch pulse",
                    color = Color(0xFF8792AA), fontSize = 11.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick = { if (valid) onStart(number!!) }, enabled = valid,
                    modifier = Modifier.fillMaxWidth()) { Text("START PLAYING") }
            }
        }
    }
}
