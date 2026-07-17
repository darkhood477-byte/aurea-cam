import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

# 1. Update the canvas where the focus ring is drawn
target_canvas = """                tapFocusPoint?.let { point ->
                    drawCircle(
                        color = com.example.ui.theme.Orange500.copy(alpha = focusPulseAlpha.value),
                        radius = focusPulseRadius.value,
                        center = point,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                }"""

replacement_canvas = """                tapFocusPoint?.let { point ->
                    val color = com.example.ui.theme.Orange500.copy(alpha = focusPulseAlpha.value)
                    val size = focusPulseRadius.value * 2
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(point.x - size / 2, point.y - size / 2),
                        size = androidx.compose.ui.geometry.Size(size, size),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                    // Draw a sun icon next to it
                    val sunX = point.x + size / 2 + 16f
                    val sunY = point.y
                    drawCircle(
                        color = color,
                        radius = 12f,
                        center = androidx.compose.ui.geometry.Offset(sunX, sunY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                    // draw lines for the sun
                    for (i in 0 until 8) {
                        val angle = (i * 45) * Math.PI / 180
                        val startX = sunX + kotlin.math.cos(angle).toFloat() * 16f
                        val startY = sunY + kotlin.math.sin(angle).toFloat() * 16f
                        val endX = sunX + kotlin.math.cos(angle).toFloat() * 22f
                        val endY = sunY + kotlin.math.sin(angle).toFloat() * 22f
                        drawLine(
                            color = color,
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = 3f
                        )
                    }
                }"""
content = content.replace(target_canvas, replacement_canvas)

# 2. Add an exposure offset state and a drag gesture to change it
# First find tapFocusPoint
target_state = "    var tapFocusPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }"
replacement_state = """    var tapFocusPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var exposureCompensation by remember { mutableStateOf(0f) }"""
content = content.replace(target_state, replacement_state)

open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
