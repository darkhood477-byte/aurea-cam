package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlin.math.abs
import kotlin.math.atan2

@Composable
fun VirtualHorizon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var pitch by remember { mutableStateOf(0f) }
    var roll by remember { mutableStateOf(0f) }
    var wasLevel by remember { mutableStateOf(false) }

    DisposableEffect(sensorManager) {
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    
                    val computedPitch = Math.toDegrees(atan2(z.toDouble(), y.toDouble())).toFloat()
                    val computedRoll = Math.toDegrees(atan2(x.toDouble(), Math.hypot(y.toDouble(), z.toDouble()))).toFloat()
                    
                    roll = computedRoll
                    pitch = computedPitch
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    val animatedRoll by animateFloatAsState(targetValue = roll, label = "roll")
    val animatedPitch by animateFloatAsState(targetValue = pitch, label = "pitch")
    
    val isLevel = abs(animatedRoll) < 1.0f && abs(animatedPitch) < 2.0f
    
    LaunchedEffect(isLevel) {
        if (isLevel && !wasLevel) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        wasLevel = isLevel
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // If pointing down/up, we use a different layout, but for now just hide the horizon if pitch is extreme
        // We will show a crosshair if it's pointing down (table top shot)
        val isTableTop = abs(animatedPitch) > 75f
        
        val color = if (isLevel) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.7f)
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        if (isTableTop) {
            // Table top mode: show two crosshairs that need to overlap
            val fixedCrossSize = 15f
            // Fixed reference cross
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(centerX - fixedCrossSize, centerY),
                end = Offset(centerX + fixedCrossSize, centerY),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(centerX, centerY - fixedCrossSize),
                end = Offset(centerX, centerY + fixedCrossSize),
                strokeWidth = 3f
            )
            
            // Moving cross
            // For tabletop, pitch is near 90 or -90. We use X and Y gravity components for offset
            // When perfectly flat, X=0, Y=0, Z=9.8
            val dx = animatedRoll * 10f // approximation
            val dy = if (animatedPitch > 0) (90f - animatedPitch) * 10f else (-90f - animatedPitch) * 10f
            
            val movingCrossSize = 15f
            drawLine(
                color = color,
                start = Offset(centerX - movingCrossSize + dx, centerY + dy),
                end = Offset(centerX + movingCrossSize + dx, centerY + dy),
                strokeWidth = 3f
            )
            drawLine(
                color = color,
                start = Offset(centerX + dx, centerY - movingCrossSize + dy),
                end = Offset(centerX + dx, centerY + movingCrossSize + dy),
                strokeWidth = 3f
            )
        } else {
            // Standard upright horizon mode
            val gap = 30f
            val lineLength = 50f
            val stroke = 3f

            // Fixed reference line
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(centerX - gap - lineLength, centerY),
                end = Offset(centerX - gap, centerY),
                strokeWidth = stroke
            )
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(centerX + gap, centerY),
                end = Offset(centerX + gap + lineLength, centerY),
                strokeWidth = stroke
            )
            
            // Moving level line
            withTransform({
                rotate(degrees = animatedRoll, pivot = Offset(centerX, centerY))
                translate(top = animatedPitch * 15f) // Adjust multiplier for sensitivity
            }) {
                // If it is level, we draw one continuous line.
                if (isLevel) {
                    drawLine(
                        color = color,
                        start = Offset(centerX - gap - lineLength, centerY),
                        end = Offset(centerX + gap + lineLength, centerY),
                        strokeWidth = stroke
                    )
                } else {
                    drawLine(
                        color = color,
                        start = Offset(centerX - gap - lineLength, centerY),
                        end = Offset(centerX + gap + lineLength, centerY),
                        strokeWidth = stroke
                    )
                }
            }
        }
    }
}
