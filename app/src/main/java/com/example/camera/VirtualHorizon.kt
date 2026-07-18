package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import android.view.Surface
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlin.math.abs

@Composable
fun VirtualHorizon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var pitch by remember { mutableStateOf(0f) }
    var roll by remember { mutableStateOf(0f) }
    var wasLevel by remember { mutableStateOf(false) }

    DisposableEffect(sensorManager) {
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val listener = object : SensorEventListener {
            private var gravityValues: FloatArray? = null
            private var geomagneticValues: FloatArray? = null

            override fun onSensorChanged(event: SensorEvent) {
                var R: FloatArray? = null
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rMatrix, event.values)
                    R = rMatrix
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        gravityValues = event.values.clone()
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        geomagneticValues = event.values.clone()
                    }
                    val g = gravityValues
                    val m = geomagneticValues
                    if (g != null && m != null) {
                        val rMatrix = FloatArray(9)
                        val iMatrix = FloatArray(9)
                        if (SensorManager.getRotationMatrix(rMatrix, iMatrix, g, m)) {
                            R = rMatrix
                        }
                    }
                }

                if (R != null) {
                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        try {
                            context.display?.rotation ?: Surface.ROTATION_0
                        } catch (e: Exception) {
                            windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                        }
                    } else {
                        windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                    }

                    val (axisX, axisY) = when (rotation) {
                        Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X)
                        Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y)
                        Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X)
                        else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Y)
                    }

                    val outR = FloatArray(9)
                    if (SensorManager.remapCoordinateSystem(R, axisX, axisY, outR)) {
                        val orientationValues = FloatArray(3)
                        SensorManager.getOrientation(outR, orientationValues)

                        val computedPitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                        val computedRoll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

                        // Low-pass filter (exponential smoothing) to reduce sensor jitter
                        val alpha = 0.12f
                        roll = alpha * computedRoll + (1f - alpha) * roll
                        pitch = alpha * computedPitch + (1f - alpha) * pitch
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val animatedRoll by animateFloatAsState(targetValue = roll, label = "roll")
    val animatedPitch by animateFloatAsState(targetValue = pitch, label = "pitch")

    val isTableTop = abs(animatedPitch) > 75f
    val isLevel = if (isTableTop) {
        abs(animatedRoll) < 1.0f && (90f - abs(animatedPitch)) < 1.0f
    } else {
        abs(animatedRoll) < 1.0f
    }

    LaunchedEffect(isLevel) {
        if (isLevel && !wasLevel) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        wasLevel = isLevel
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val color = if (isLevel) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.7f)
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        if (isTableTop) {
            // Table top mode: Concentric bulls-eye spirit level
            val outerRadius = 60f
            val innerRadius = 20f
            val bubbleRadius = 14f

            // Draw outer target circle
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = outerRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3f)
            )

            // Draw inner target circle
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = innerRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )

            // Draw crosshairs
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(centerX - outerRadius, centerY),
                end = Offset(centerX + outerRadius, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(centerX, centerY - outerRadius),
                end = Offset(centerX, centerY + outerRadius),
                strokeWidth = 2f
            )

            // Map roll/pitch to X/Y offset for flat bubble movement
            // Roll moves bubble along X-axis, Pitch moves bubble along Y-axis
            val maxOffset = outerRadius - bubbleRadius
            val dx = (-animatedRoll * 4f).coerceIn(-maxOffset, maxOffset)
            val dy = (if (animatedPitch > 0) (90f - animatedPitch) * 4f else (-90f - animatedPitch) * -4f).coerceIn(-maxOffset, maxOffset)

            // Draw leveling bubble
            drawCircle(
                color = color,
                radius = bubbleRadius,
                center = Offset(centerX + dx, centerY + dy)
            )
        } else {
            // Upright standard horizon and bubble spirit vial level
            val gap = 30f
            val lineLength = 50f
            val stroke = 3f

            // 1. Static Horizontal Reference Ticks
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

            // 2. Rotating/Tilting Horizon Line
            withTransform({
                rotate(degrees = animatedRoll, pivot = Offset(centerX, centerY))
                translate(top = (animatedPitch % 90f) * 1.5f) // translate slightly based on pitch
            }) {
                drawLine(
                    color = color,
                    start = Offset(centerX - gap - lineLength, centerY),
                    end = Offset(centerX + gap + lineLength, centerY),
                    strokeWidth = stroke
                )
            }

            // 3. Horizontal Bubble Spirit Vial (vial capsule and moving bubble)
            val vialWidth = 140f
            val vialHeight = 24f
            val vialLeft = centerX - vialWidth / 2f
            val vialTop = centerY + 80f - vialHeight / 2f

            // Draw vial rounded glass capsule
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(vialLeft, vialTop),
                size = Size(vialWidth, vialHeight),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 2f)
            )

            // Draw vial center target markings
            val markerGap = 16f
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(centerX - markerGap, vialTop),
                end = Offset(centerX - markerGap, vialTop + vialHeight),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(centerX + markerGap, vialTop),
                end = Offset(centerX + markerGap, vialTop + vialHeight),
                strokeWidth = 2f
            )

            // Shift bubble based on roll angle (bubble moves opposite of tilt)
            val maxBubbleOffset = (vialWidth / 2f) - 10f
            val bubbleOffset = (-animatedRoll * 4f).coerceIn(-maxBubbleOffset, maxBubbleOffset)

            // Draw the spirit bubble
            drawCircle(
                color = if (isLevel) color else Color.White.copy(alpha = 0.8f),
                radius = 8f,
                center = Offset(centerX + bubbleOffset, centerY + 80f)
            )
        }
    }
}
