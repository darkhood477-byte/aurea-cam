package com.example.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Details

import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Crop75
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

enum class CameraTimer(val label: String, val seconds: Int) {
    OFF("OFF", 0),
    SEC_3("3s", 3),
    SEC_10("10s", 10)
}

enum class AspectRatioMode(val label: String, val ratio: Float?) {
    RATIO_9_16("9:16", 9f/16f),
    RATIO_3_4("3:4", 3f/4f),
    RATIO_1_1("1:1", 1f),
    RATIO_4_3("4:3", 4f/3f),
    RATIO_16_9("16:9", 16f/9f),
    FULL("FULL", null)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (permissionsState.allPermissionsGranted) {
        CameraContent()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Permissions required to use camera and audio.", color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
fun CameraContent() {
    val context = LocalContext.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var compositionOverlay by remember { mutableStateOf(CompositionOverlay.NONE) }
    var isRecording by remember { mutableStateOf(false) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }
    var isVideoMode by remember { mutableStateOf(true) } // Default to video for this vlog aesthetic
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.RATIO_9_16) }
    
    var timerMode by remember { mutableStateOf(CameraTimer.OFF) }
    var isGlobalSettingsExpanded by remember { mutableStateOf(false) }
    var resolution by remember { mutableStateOf("4K") }
    var frameRate by remember { mutableStateOf("60 fps") }
    var zebraStripes by remember { mutableStateOf(false) }
    var audioSource by remember { mutableStateOf("Internal") }
    var isVirtualHorizonEnabled by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf<Int?>(null) }
    var linearZoom by remember { mutableStateOf(0f) }
    
    var isSettingsExpanded by remember { mutableStateOf(false) }
    var isoValue by remember { mutableStateOf(400f) }
    var shutterValue by remember { mutableStateOf(125f) }
    var wbValue by remember { mutableStateOf(5600f) }
    var focusDistance by remember { mutableStateOf<Float?>(null) }
    var isExposureLocked by remember { mutableStateOf(false) }
    var tapFocusPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var exposureCompensation by remember { mutableStateOf(0f) }
    val focusPulseRadius = remember { androidx.compose.animation.core.Animatable(0f) }
    val focusPulseAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    
    LaunchedEffect(tapFocusPoint) {
        if (tapFocusPoint != null) {
            focusPulseRadius.snapTo(80f)
            focusPulseAlpha.snapTo(1f)
            focusPulseRadius.animateTo(
                targetValue = 40f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f)
            )
        }
    }

    LaunchedEffect(tapFocusPoint, isExposureLocked, exposureCompensation) {
        if (tapFocusPoint != null) {
            focusPulseAlpha.snapTo(1f)
            if (!isExposureLocked) {
                kotlinx.coroutines.delay(1500)
                focusPulseAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
                tapFocusPoint = null
            }
        }
    }
    
    val scope = rememberCoroutineScope()

    val cameraSelector = remember(lensFacing) {
        try {
            CameraSelector.Builder().requireLensFacing(lensFacing).build()
        } catch(e: Exception) {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    val imageCapture = remember(aspectRatioMode) {
        val cxRatio = when (aspectRatioMode) {
            AspectRatioMode.RATIO_16_9, AspectRatioMode.RATIO_9_16 -> androidx.camera.core.AspectRatio.RATIO_16_9
            AspectRatioMode.RATIO_4_3, AspectRatioMode.RATIO_3_4 -> androidx.camera.core.AspectRatio.RATIO_4_3
            else -> androidx.camera.core.AspectRatio.RATIO_16_9
        }
        ImageCapture.Builder()
            .setTargetAspectRatio(cxRatio)
            .build()
    }
    
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Main camera preview area
         BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = constraints.maxWidth.toFloat()
            val screenHeight = constraints.maxHeight.toFloat()
            val screenRatio = if (screenHeight > 0) screenWidth / screenHeight else 1f
            val targetRatio = when (aspectRatioMode) {
                AspectRatioMode.RATIO_9_16 -> 9f / 16f
                AspectRatioMode.RATIO_3_4 -> 3f / 4f
                AspectRatioMode.RATIO_1_1 -> 1f
                AspectRatioMode.RATIO_4_3 -> 4f / 3f
                AspectRatioMode.RATIO_16_9 -> 16f / 9f
                AspectRatioMode.FULL -> screenRatio
            }

            // Calculate target container size to fit perfectly inside the BoxWithConstraints bounding box
            val maxContainerWidth = this.maxWidth
            val maxContainerHeight = this.maxHeight
            val containerRatio = if (maxContainerHeight.value > 0) maxContainerWidth.value / maxContainerHeight.value else 1f
            
            val (targetWidth, targetHeight) = if (containerRatio < targetRatio) {
                val w = maxContainerWidth
                val h = maxContainerWidth / targetRatio
                Pair(w, h)
            } else {
                val h = maxContainerHeight
                val w = maxContainerHeight * targetRatio
                Pair(w, h)
            }

            // Smoothly animate both width and height (equivalent to smooth CSS transitions)
            val animatedWidth by androidx.compose.animation.core.animateDpAsState(
                targetValue = targetWidth,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 350,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                label = "previewWidthAnimation"
            )
            val animatedHeight by androidx.compose.animation.core.animateDpAsState(
                targetValue = targetHeight,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 350,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                label = "previewHeightAnimation"
            )

            // Centers and smoothly resizes the active camera preview and its overlays (equivalent to smooth CSS transitions)
            Box(
                modifier = Modifier
                    .size(animatedWidth, animatedHeight)
                    .align(Alignment.Center)
                    .background(Color.Black)
                    .clipToBounds()
            ) {
                CameraPreviewWithUseCases(
                    modifier = Modifier.fillMaxSize(),
                    cameraSelector = cameraSelector,
                    imageCapture = imageCapture,
                    videoCapture = videoCapture,
                    isVideoMode = isVideoMode,
                    aspectRatioMode = aspectRatioMode,
                    focusDistance = focusDistance,
                    isExposureLocked = isExposureLocked,
                    exposureCompensation = exposureCompensation,
                    linearZoom = linearZoom,
                    onTapToFocus = { x, y ->
                        isExposureLocked = false
                        exposureCompensation = 0f
                        tapFocusPoint = androidx.compose.ui.geometry.Offset(x, y)
                    },
                    onLongPressToLock = { x, y ->
                        isExposureLocked = true
                        tapFocusPoint = androidx.compose.ui.geometry.Offset(x, y)
                    },
                    onExposureChange = { dy ->
                        exposureCompensation = (exposureCompensation - dy * 0.05f).coerceIn(-12f, 12f)
                    }
                )

                // Virtual Horizon inside active viewport
                if (isVirtualHorizonEnabled) {
                    VirtualHorizon(modifier = Modifier.matchParentSize())
                }

                // Composition Overlays inside active viewport (without risk of being covered)
                androidx.compose.animation.Crossfade(
                    targetState = compositionOverlay,
                    animationSpec = androidx.compose.animation.core.tween(300),
                    label = "overlayCrossfade"
                ) { currentOverlay ->
                    CompositionOverlayCanvas(overlay = currentOverlay, aspectRatio = null)
                }

                // Central crosshair centered in active viewport
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(color = Color.White.copy(alpha = 0.1f), start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height), strokeWidth = 1f)
                    drawLine(color = Color.White.copy(alpha = 0.1f), start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = 1f)
                    
                    tapFocusPoint?.let { point ->
                        val color = com.example.ui.theme.Orange500.copy(alpha = focusPulseAlpha.value)
                        val size = focusPulseRadius.value * 2
                        drawRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(point.x - size / 2, point.y - size / 2),
                            size = androidx.compose.ui.geometry.Size(size, size),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                        // Draw a sun icon next to it
                        val sunX = point.x + size / 2 + 32f
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
                        }
                    }
                }
                
                // AE/AF LOCK Badge inside active viewport
                if (isExposureLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 90.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(com.example.ui.theme.Orange500)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("AE/AF LOCK", color = Color.Black, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }

                // Stabilization active text centered at the bottom of active viewport
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.width(48.dp).height(1.dp).background(Color.White.copy(alpha = 0.4f)))
                    Text("STABILIZATION ACTIVE", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                }

                if (zebraStripes) {
                    ZebraStripesOverlay(modifier = Modifier.matchParentSize())
                }
            }

        // Left side settings panel
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 80.dp, start = 16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        "ISO" to isoValue.toInt().toString(), 
                        "SHUTTER" to "1/${shutterValue.toInt()}", 
                        "WB" to "${wbValue.toInt()}K",
                        "FOCUS" to if (focusDistance == null) "AF" else "MF",
                        "METER" to "MATRIX"
                    ).forEach { (label, value) ->
                        Column(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { isSettingsExpanded = !isSettingsExpanded }
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(label, color = com.example.ui.theme.Neutral400, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(value, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSettingsExpanded,
                    enter = androidx.compose.animation.expandHorizontally(expandFrom = Alignment.Start) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.Start) + androidx.compose.animation.fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // ISO Slider
                        Column {
                            Text("ISO: ${isoValue.toInt()}", color = Color.White, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Slider(
                                value = isoValue,
                                onValueChange = { isoValue = it },
                                valueRange = 100f..3200f,
                                modifier = Modifier.width(160.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = com.example.ui.theme.Orange500)
                            )
                        }
                        // Shutter Slider
                        Column {
                            Text("SHUTTER: 1/${shutterValue.toInt()}", color = Color.White, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Slider(
                                value = shutterValue,
                                onValueChange = { shutterValue = it },
                                valueRange = 30f..1000f,
                                modifier = Modifier.width(160.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = com.example.ui.theme.Orange500)
                            )
                        }
                        // WB Slider
                        Column {
                            Text("WB: ${wbValue.toInt()}K", color = Color.White, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Slider(
                                value = wbValue,
                                onValueChange = { wbValue = it },
                                valueRange = 2000f..8000f,
                                modifier = Modifier.width(160.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = com.example.ui.theme.Orange500)
                            )
                        }
                        // Focus Slider
                        Column {
                            Text("FOCUS: ${if (focusDistance == null) "AUTO" else String.format(java.util.Locale.US, "%.2f", focusDistance)}", color = Color.White, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Slider(
                                value = focusDistance ?: 0f,
                                onValueChange = { focusDistance = if (it < 0.1f) null else it },
                                valueRange = 0f..10f,
                                modifier = Modifier.width(160.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = com.example.ui.theme.Orange500)
                            )
                        }

                    }
                }
            }

            // Right side exposure slider
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(128.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 32.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Text(
                    "EXPOSURE",
                    color = com.example.ui.theme.Neutral500,
                    fontSize = 9.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.rotate(-90f).padding(top = 24.dp)
                )
            }
            
            // Countdown Overlay
            if (countdownValue != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdownValue.toString(),
                        color = Color.White,
                        fontSize = 120.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PROJECT", color = com.example.ui.theme.Neutral500, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 2.sp)
                Text("VLOG_042_NYC", color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("REC TIME", color = com.example.ui.theme.Neutral500, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 2.sp)
                    Text("00:12:44:02", color = if (isRecording) com.example.ui.theme.Red500 else Color.White, fontSize = 14.sp)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { isVirtualHorizonEnabled = !isVirtualHorizonEnabled }
                ) {
                    Icon(Icons.Filled.ScreenRotation, contentDescription = "Level", tint = if (isVirtualHorizonEnabled) com.example.ui.theme.Orange500 else Color.White, modifier = Modifier.size(20.dp))
                    Text("LEVEL", color = if (isVirtualHorizonEnabled) com.example.ui.theme.Orange500 else Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        timerMode = when (timerMode) {
                            CameraTimer.OFF -> CameraTimer.SEC_3
                            CameraTimer.SEC_3 -> CameraTimer.SEC_10
                            CameraTimer.SEC_10 -> CameraTimer.OFF
                        }
                    }
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = "Timer", tint = if (timerMode != CameraTimer.OFF) com.example.ui.theme.Orange500 else Color.White, modifier = Modifier.size(20.dp))
                    Text(timerMode.label, color = if (timerMode != CameraTimer.OFF) com.example.ui.theme.Orange500 else Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Icon(
                    Icons.Filled.Settings, 
                    contentDescription = "Settings", 
                    tint = if (isGlobalSettingsExpanded) com.example.ui.theme.Orange500 else Color.White, 
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { isGlobalSettingsExpanded = !isGlobalSettingsExpanded }
                )
            }
        }

        // Global Settings Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = isGlobalSettingsExpanded,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier.zIndex(100f)
        ) {
            SettingsSheet(
                onDismiss = { isGlobalSettingsExpanded = false },
                resolution = resolution,
                onResolutionChange = { resolution = it },
                frameRate = frameRate,
                onFrameRateChange = { frameRate = it },
                zebraStripes = zebraStripes,
                onZebraStripesChange = { zebraStripes = it },
                audioSource = audioSource,
                onAudioSourceChange = { audioSource = it }
            )
        }
        
        // Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        ) {
            // Overlays Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val overlays = listOf(
                    "THIRDS" to (CompositionOverlay.RULE_OF_THIRDS to Icons.Filled.Grid3x3),
                    "PHI GRID" to (CompositionOverlay.GOLDEN_RATIO to Icons.Filled.Grid4x4),
                    "SPIRAL" to (CompositionOverlay.GOLDEN_SPIRAL to Icons.Filled.Circle),
                    "TRIANGLE" to (CompositionOverlay.GOLDEN_TRIANGLE to Icons.Filled.ChangeHistory),
                    "SYMMETRY" to (CompositionOverlay.DYNAMIC_SYMMETRY to Icons.Filled.Details)
                )
                
                overlays.forEach { (label, pair) ->
                    val overlay = pair.first
                    val icon = pair.second
                    val isActive = compositionOverlay == overlay
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { 
                                compositionOverlay = if (isActive) CompositionOverlay.NONE else overlay 
                            }
                            .padding(8.dp)
                    ) {
                        Icon(icon, contentDescription = label, tint = if (isActive) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, color = if (isActive) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }

            // Dedicated Aspect Ratio Segmented Picker Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ratios = listOf(
                        AspectRatioMode.RATIO_9_16 to "9:16",
                        AspectRatioMode.RATIO_3_4 to "3:4",
                        AspectRatioMode.RATIO_1_1 to "1:1",
                        AspectRatioMode.RATIO_4_3 to "4:3",
                        AspectRatioMode.RATIO_16_9 to "16:9",
                        AspectRatioMode.FULL to "FULL"
                    )
                    ratios.forEach { (mode, label) ->
                        val isSelected = aspectRatioMode == mode
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) com.example.ui.theme.Orange500 else Color.Transparent)
                                .clickable { aspectRatioMode = mode }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PHOTO",
                    color = if (!isVideoMode) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable { if (!isRecording) isVideoMode = false }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    "VIDEO",
                    color = if (isVideoMode) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable { if (!isRecording) isVideoMode = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Zoom Control Slider Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Zoom Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1fx", 1.0f + linearZoom * 7.0f),
                        color = com.example.ui.theme.Orange500,
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { linearZoom = (linearZoom - 0.1f).coerceIn(0f, 1f) }
                    )
                    
                    Slider(
                        value = linearZoom,
                        onValueChange = { linearZoom = it },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = com.example.ui.theme.Orange500,
                            activeTrackColor = com.example.ui.theme.Orange500,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    
                    Icon(
                        Icons.Filled.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { linearZoom = (linearZoom + 0.1f).coerceIn(0f, 1f) }
                    )
                }
            }

            // Capture Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Icon Mock
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(com.example.ui.theme.Neutral800)
                        .clickable { /* TODO: Open gallery */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Image, contentDescription = "Gallery", tint = com.example.ui.theme.Neutral500)
                }

                // Shutter
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (countdownValue != null) return@clickable // Ignore clicks during countdown
                            
                            val startAction = {
                                if (isVideoMode) {
                                    startVideoRecording(context, videoCapture) { recording ->
                                        currentRecording = recording
                                        isRecording = true
                                    }
                                } else {
                                    takePhoto(context, imageCapture)
                                }
                            }
                            
                            if (isVideoMode && isRecording) {
                                currentRecording?.stop()
                                isRecording = false
                            } else if (timerMode != CameraTimer.OFF) {
                                scope.launch {
                                    for (i in timerMode.seconds downTo 1) {
                                        countdownValue = i
                                        delay(1000)
                                    }
                                    countdownValue = null
                                    startAction()
                                }
                            } else {
                                startAction()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Glow effect
                    val glowColor = if (isVideoMode) com.example.ui.theme.Red500.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(glowColor))
                    // White border
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        // Inner Black Gap
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                            // Core
                            Box(
                                modifier = Modifier
                                    .size(if (isRecording) 28.dp else 52.dp)
                                    .clip(if (isRecording) MaterialTheme.shapes.small else CircleShape)
                                    .background(if (isVideoMode) com.example.ui.theme.Red600 else Color.White)
                            )
                        }
                    }
                }

                // Flip Camera
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.FlipCameraIos, contentDescription = "Flip", tint = Color.White)
                }
            }
        }
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    // For simplicity, we just save to app's files directory or media store.
    // In a real app we'd use MediaStore to save to gallery.
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                Toast.makeText(context, "Photo capture failed", Toast.LENGTH_SHORT).show()
            }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val msg = "Photo capture succeeded: ${output.savedUri}"
                Toast.makeText(context, "Saved Photo", Toast.LENGTH_SHORT).show()
                Log.d("CameraScreen", msg)
            }
        }
    )
}

private fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    onRecordingStarted: (Recording) -> Unit
) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, name)
    }

    val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    // Assuming permissions are already granted.
    try {
        val recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d("CameraScreen", "Video recording started")
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: ${recordEvent.outputResults.outputUri}"
                            Toast.makeText(context, "Saved Video", Toast.LENGTH_SHORT).show()
                            Log.d("CameraScreen", msg)
                        } else {
                            Log.e("CameraScreen", "Video capture ends with error: ${recordEvent.error}")
                        }
                    }
                }
            }
        onRecordingStarted(recording)
    } catch (e: SecurityException) {
        Log.e("CameraScreen", "SecurityException creating recording", e)
    }
}
