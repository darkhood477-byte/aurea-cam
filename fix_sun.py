import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

target = """                    // Draw a sun icon next to it
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
                    }"""

replacement = """                    // Draw a sun icon next to it
                    val sunX = point.x + size / 2 + 32f
                    // exposureCompensation goes from -12 to 12. 
                    // Negative is darker (sun moves down), Positive is brighter (sun moves up).
                    // So -exposureCompensation maps to vertical offset.
                    val sliderOffset = -(exposureCompensation / 12f) * 60f
                    val sunY = point.y + sliderOffset
                    
                    // Draw a thin line for the slider track
                    drawLine(
                        color = color.copy(alpha = 0.5f),
                        start = androidx.compose.ui.geometry.Offset(sunX, point.y - 60f),
                        end = androidx.compose.ui.geometry.Offset(sunX, point.y + 60f),
                        strokeWidth = 2f
                    )
                    
                    drawCircle(
                        color = color,
                        radius = 10f,
                        center = androidx.compose.ui.geometry.Offset(sunX, sunY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                    // draw lines for the sun
                    for (i in 0 until 8) {
                        val angle = (i * 45) * Math.PI / 180
                        val startX = sunX + kotlin.math.cos(angle).toFloat() * 14f
                        val startY = sunY + kotlin.math.sin(angle).toFloat() * 14f
                        val endX = sunX + kotlin.math.cos(angle).toFloat() * 18f
                        val endY = sunY + kotlin.math.sin(angle).toFloat() * 18f
                        drawLine(
                            color = color,
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = 2f
                        )
                    }"""
content = content.replace(target, replacement)
open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
print("Updated sun icon")
