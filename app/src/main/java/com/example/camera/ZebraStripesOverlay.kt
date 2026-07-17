package com.example.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect

@Composable
fun ZebraStripesOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        clipRect {
            val stripeWidth = 40f
            val maxDim = maxOf(size.width, size.height) * 2
            
            var x = -maxDim
            while (x < maxDim) {
                val path = Path().apply {
                    moveTo(x, 0f)
                    lineTo(x + stripeWidth, 0f)
                    lineTo(x + stripeWidth + size.height, size.height)
                    lineTo(x + size.height, size.height)
                    close()
                }
                
                // We'll just draw some faint diagonal stripes as a visual approximation
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.15f)
                )
                
                x += stripeWidth * 2
            }
        }
    }
}
