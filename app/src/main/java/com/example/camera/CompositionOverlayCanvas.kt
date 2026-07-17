package com.example.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipRect

@Composable
fun CompositionOverlayCanvas(overlay: CompositionOverlay, aspectRatio: Float? = null) {
    val overlayColor = Color.White.copy(alpha = 0.6f)
    val strokeWidth = 2f

    Canvas(modifier = Modifier.fillMaxSize()) {
        var cropWidth = size.width
        var cropHeight = size.height
        var dx = 0f
        var dy = 0f

        if (aspectRatio != null) {
            val screenRatio = size.width / size.height
            if (screenRatio < aspectRatio) {
                cropHeight = size.width / aspectRatio
                dy = (size.height - cropHeight) / 2
            } else {
                cropWidth = size.height * aspectRatio
                dx = (size.width - cropWidth) / 2
            }
        }

        withTransform({
            translate(left = dx, top = dy)
            clipRect(left = 0f, top = 0f, right = cropWidth, bottom = cropHeight)
        }) {
            val width = cropWidth
            val height = cropHeight

            when (overlay) {
                CompositionOverlay.RULE_OF_THIRDS -> {
                    drawLine(
                        color = overlayColor,
                        start = Offset(width / 3, 0f),
                        end = Offset(width / 3, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = overlayColor,
                        start = Offset(width * 2 / 3, 0f),
                        end = Offset(width * 2 / 3, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = overlayColor,
                        start = Offset(0f, height / 3),
                        end = Offset(width, height / 3),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = overlayColor,
                        start = Offset(0f, height * 2 / 3),
                        end = Offset(width, height * 2 / 3),
                        strokeWidth = strokeWidth
                    )
                }
                CompositionOverlay.GOLDEN_RATIO -> {
                    val phi = 1.6180339887f
                    val phi1 = 1f - 1f / phi
                    val phi2 = 1f / phi
                    drawLine(
                        color = overlayColor,
                        start = Offset(width * phi1, 0f),
                        end = Offset(width * phi1, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = overlayColor,
                        start = Offset(width * phi2, 0f),
                        end = Offset(width * phi2, height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = overlayColor,
                        start = Offset(0f, height * phi1),
                        end = Offset(width, height * phi1),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = overlayColor,
                        start = Offset(0f, height * phi2),
                        end = Offset(width, height * phi2),
                        strokeWidth = strokeWidth
                    )
                }
                CompositionOverlay.GOLDEN_SPIRAL -> {
                    val path = Path()
                    path.moveTo(0f, 0f)
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.000000f,
                            top = height * -1.000000f,
                            right = width * 1.236068f,
                            bottom = height * 1.000000f
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.236068f,
                            top = height * -0.236068f,
                            right = width * 1.000000f,
                            bottom = height * 1.000000f
                        ),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.527864f,
                            top = height * 0.000000f,
                            right = width * 1.000000f,
                            bottom = height * 0.763932f
                        ),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.618034f,
                            top = height * 0.000000f,
                            right = width * 0.909830f,
                            bottom = height * 0.472136f
                        ),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.618034f,
                            top = height * 0.090170f,
                            right = width * 0.798374f,
                            bottom = height * 0.381966f
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.652476f,
                            top = height * 0.201626f,
                            right = width * 0.763932f,
                            bottom = height * 0.381966f
                        ),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.695048f,
                            top = height * 0.236068f,
                            right = width * 0.763932f,
                            bottom = height * 0.347524f
                        ),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.708204f,
                            top = height * 0.236068f,
                            right = width * 0.750776f,
                            bottom = height * 0.304952f
                        ),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.708204f,
                            top = height * 0.249224f,
                            right = width * 0.734515f,
                            bottom = height * 0.291796f
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = width * 0.713229f,
                            top = height * 0.265485f,
                            right = width * 0.729490f,
                            bottom = height * 0.291796f
                        ),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    drawPath(path, color = overlayColor, style = Stroke(width = strokeWidth))
                }
                CompositionOverlay.GOLDEN_TRIANGLE -> {
                    // Diagonal
                    drawLine(color = overlayColor, start = Offset(0f, 0f), end = Offset(width, height), strokeWidth = strokeWidth)
                    // Perpendicular from top-right
                    val x1 = width * width * width / (height * height + width * width)
                    val y1 = height * width * width / (height * height + width * width)
                    drawLine(color = overlayColor, start = Offset(width, 0f), end = Offset(x1, y1), strokeWidth = strokeWidth)
                    // Perpendicular from bottom-left
                    val x2 = width - x1
                    val y2 = height - y1
                    drawLine(color = overlayColor, start = Offset(0f, height), end = Offset(x2, y2), strokeWidth = strokeWidth)
                }
                CompositionOverlay.DYNAMIC_SYMMETRY -> {
                    // Diagonals
                    drawLine(color = overlayColor, start = Offset(0f, 0f), end = Offset(width, height), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(width, 0f), end = Offset(0f, height), strokeWidth = strokeWidth)
                    // Reciprocal lines intersecting the main diagonal
                    val x1 = width * width * width / (height * height + width * width)
                    val y1 = height * width * width / (height * height + width * width)
                    drawLine(color = overlayColor, start = Offset(width, 0f), end = Offset(x1, y1), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(0f, height), end = Offset(width - x1, height - y1), strokeWidth = strokeWidth)
                    
                    val x3 = width * width * width / (height * height + width * width)
                    val y3 = height - (height * width * width / (height * height + width * width))
                    drawLine(color = overlayColor, start = Offset(0f, 0f), end = Offset(width - x3, y3), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(width, height), end = Offset(x3, height - y3), strokeWidth = strokeWidth)
                }
                CompositionOverlay.LEADING_LINES -> {
                    // Perspective lines leading to center
                    drawLine(color = overlayColor, start = Offset(0f, height), end = Offset(width / 2, height / 2), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(width, height), end = Offset(width / 2, height / 2), strokeWidth = strokeWidth)
                }
                CompositionOverlay.FRAMING -> {
                    drawRect(
                        color = overlayColor,
                        topLeft = Offset(width * 0.1f, height * 0.1f),
                        size = Size(width * 0.8f, height * 0.8f),
                        style = Stroke(width = strokeWidth)
                    )
                }
                CompositionOverlay.SYMMETRY_CENTERING -> {
                    // Crosshair
                    drawLine(color = overlayColor, start = Offset(width / 2, 0f), end = Offset(width / 2, height), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(0f, height / 2), end = Offset(width, height / 2), strokeWidth = strokeWidth)
                }
                CompositionOverlay.RADIAL_SYMMETRY -> {
                    // Radiating lines from center
                    val cx = width / 2
                    val cy = height / 2
                    drawLine(color = overlayColor, start = Offset(cx, 0f), end = Offset(cx, height), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(0f, cy), end = Offset(width, cy), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(0f, 0f), end = Offset(width, height), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(width, 0f), end = Offset(0f, height), strokeWidth = strokeWidth)
                }
                CompositionOverlay.VANISHING_POINT -> {
                    // Multiple lines converging at a point (let's say bottom-middle or center)
                    val vpX = width / 2
                    val vpY = height * 0.4f
                    drawLine(color = overlayColor, start = Offset(0f, height), end = Offset(vpX, vpY), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(width, height), end = Offset(vpX, vpY), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(0f, height * 0.6f), end = Offset(vpX, vpY), strokeWidth = strokeWidth)
                    drawLine(color = overlayColor, start = Offset(width, height * 0.6f), end = Offset(vpX, vpY), strokeWidth = strokeWidth)
                }
                CompositionOverlay.NONE -> {}
            }
        }
    }
}
