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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.with
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Edit

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
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

enum class CameraTimer(val label: String, val seconds: Int) {
    OFF("OFF", 0),
    SEC_3("3s", 3),
    SEC_10("10s", 10)
}

enum class UtilitySubPanel {
    NONE,
    ASPECT,
    GRID,
    MANUAL
}

enum class AspectRatioMode(val label: String, val ratio: Float?) {
    RATIO_9_16("9:16", 9f/16f),
    RATIO_3_4("3:4", 3f/4f),
    RATIO_1_1("1:1", 1f),
    RATIO_4_3("4:3", 4f/3f),
    RATIO_16_9("16:9", 16f/9f),
    FULL("FULL", null)
}

enum class GhostOverlaySource {
    LATEST_CAPTURE,
    CUSTOM_FILE
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val galleryPermissionsState = rememberMultiplePermissionsState(
        permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            listOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )
    
    var isCapturingInProgress by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var compositionOverlay by remember { mutableStateOf(CompositionOverlay.NONE) }
    var isRecording by remember { mutableStateOf(false) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }
    var isVideoMode by remember { mutableStateOf(true) } // Default to video for this vlog aesthetic
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.RATIO_9_16) }

    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

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
    
    var isGalleryOpen by remember { mutableStateOf(false) }
    var latestMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    
    LaunchedEffect(isGalleryOpen, isRecording) {
        val media = queryMedia(context)
        if (media.isNotEmpty()) {
            latestMediaItem = media.first()
        } else {
            latestMediaItem = null
        }
    }
    
    var timerMode by remember { mutableStateOf(CameraTimer.OFF) }
    var isGlobalSettingsExpanded by remember { mutableStateOf(false) }
    var burstModeEnabled by remember { mutableStateOf(false) }
    var burstType by remember { mutableStateOf("JPEG (10fps)") }
    var isBurstCapturing by remember { mutableStateOf(false) }
    var burstProgress by remember { mutableStateOf(0) }
    var burstTotal by remember { mutableStateOf(10) }
    var resolution by remember { mutableStateOf("4K") }
    var frameRate by remember { mutableStateOf("60 fps") }
    var zebraStripes by remember { mutableStateOf(false) }
    var audioSource by remember { mutableStateOf("Internal") }
    var isVirtualHorizonEnabled by remember { mutableStateOf(false) }
    var isOverlaysEnabled by remember { mutableStateOf(false) } // Default to false so overlays default off
    var lastActivityTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var isControlStripActive by remember { mutableStateOf(true) }
    val hudAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isControlStripActive) 1f else 0.0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        label = "hudAlpha"
    )
    var activeSubPanel by remember { mutableStateOf(UtilitySubPanel.NONE) }

    var exposureRangeLower by remember { mutableStateOf(-4) }
    var exposureRangeUpper by remember { mutableStateOf(4) }
    var exposureStep by remember { mutableStateOf(0.5f) }

    var countdownValue by remember { mutableStateOf<Int?>(null) }
    var linearZoom by remember { mutableStateOf(0f) }
    
    var isoValue by remember { mutableStateOf(400f) }
    var shutterValue by remember { mutableStateOf(125f) }
    var wbValue by remember { mutableStateOf(5600f) }
    var focusDistance by remember { mutableStateOf<Float?>(null) }
    var isExposureLocked by remember { mutableStateOf(false) }
    var tapFocusPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var exposureCompensation by remember { mutableStateOf(0f) }
    var activeOverlaySetting by remember { mutableStateOf<String?>(null) }
    var lastOverlayTriggerTime by remember { mutableStateOf(0L) }

    // Flip & Ghost Mode States
    var isFrontCameraMirrored by remember { mutableStateOf(true) }
    var isGhostOverlayEnabled by remember { mutableStateOf(false) }
    var ghostOverlayOpacity by remember { mutableStateOf(0.4f) }
    var ghostOverlayCustomUri by remember { mutableStateOf<Uri?>(null) }
    var ghostOverlaySource by remember { mutableStateOf(GhostOverlaySource.LATEST_CAPTURE) }
    val ghostImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            ghostOverlayCustomUri = uri
            ghostOverlaySource = GhostOverlaySource.CUSTOM_FILE
            isGhostOverlayEnabled = true
        }
    }

    // Slider Interaction States
    val isoInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val shutterInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val wbInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focusInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    
    val isIsoPressed by isoInteractionSource.collectIsPressedAsState()
    val isShutterPressed by shutterInteractionSource.collectIsPressedAsState()
    val isWbPressed by wbInteractionSource.collectIsPressedAsState()
    val isFocusPressed by focusInteractionSource.collectIsPressedAsState()
    
    var isZoomDialDragging by remember { mutableStateOf(false) }
    
    val isSliderPressed = isIsoPressed || isShutterPressed || isWbPressed || isFocusPressed || isZoomDialDragging

    // Slate & Metadata States
    var currentScene by remember { mutableStateOf("01") }
    var currentTake by remember { mutableStateOf(1) }
    var isSlateDialogExpanded by remember { mutableStateOf(false) }
    var inputSceneName by remember { mutableStateOf("01") }
    
    LaunchedEffect(lastActivityTime, isGlobalSettingsExpanded, isSlateDialogExpanded, activeSubPanel, isSliderPressed) {
        if (isGlobalSettingsExpanded || isSlateDialogExpanded || activeSubPanel != UtilitySubPanel.NONE || isSliderPressed) {
            isControlStripActive = true
            return@LaunchedEffect
        }
        isControlStripActive = true
        delay(1500)
        isControlStripActive = false
    }

    // SMPTE Dynamic Timecode Simulation
    var recordingTimecode by remember { mutableStateOf("00:00:00:00") }

    val triggerCaptureAction: () -> Unit = triggerCaptureAction@{
        if (isCapturingInProgress) return@triggerCaptureAction
        
        if (isVideoMode) {
            if (isRecording) {
                isCapturingInProgress = true
                currentRecording?.stop()
                isRecording = false
            } else {
                isCapturingInProgress = true
                startVideoRecording(
                    context = context,
                    videoCapture = videoCapture,
                    scene = currentScene,
                    take = currentTake,
                    onRecordingStarted = { recording ->
                        currentRecording = recording
                        isRecording = true
                        isCapturingInProgress = false
                    },
                    onVideoSaved = {
                        scope.launch {
                            val media = queryMedia(context)
                            if (media.isNotEmpty()) {
                                latestMediaItem = media.first()
                            }
                        }
                        currentTake += 1
                        isCapturingInProgress = false
                    }
                )
            }
        } else {
            if (burstModeEnabled) {
                isCapturingInProgress = true
                isBurstCapturing = true
                burstProgress = 0
                val total = if (burstType.contains("RAW")) 5 else 10
                burstTotal = total
                takeBurst(
                    context = context,
                    imageCapture = imageCapture,
                    scene = currentScene,
                    startTake = currentTake,
                    burstType = burstType,
                    onProgress = { progress, total ->
                        burstProgress = progress
                        burstTotal = total
                    },
                    onComplete = { count ->
                        isBurstCapturing = false
                        isCapturingInProgress = false
                        scope.launch {
                            val media = queryMedia(context)
                            if (media.isNotEmpty()) {
                                latestMediaItem = media.first()
                            }
                        }
                        currentTake += count
                        Toast.makeText(context, "Burst completed: saved $count photos", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                isCapturingInProgress = true
                takePhoto(
                    context = context,
                    imageCapture = imageCapture,
                    scene = currentScene,
                    take = currentTake,
                    onComplete = { success ->
                        if (success) {
                            scope.launch {
                                val media = queryMedia(context)
                                if (media.isNotEmpty()) {
                                    latestMediaItem = media.first()
                                }
                            }
                            currentTake += 1
                        }
                        isCapturingInProgress = false
                    }
                )
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val startTime = System.currentTimeMillis()
            while (isRecording) {
                val elapsed = System.currentTimeMillis() - startTime
                val hours = (elapsed / (1000 * 60 * 60)) % 24
                val minutes = (elapsed / (1000 * 60)) % 60
                val seconds = (elapsed / 1000) % 60
                val frames = (elapsed % 1000) / 41 // Approx 24 fps
                recordingTimecode = String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, frames)
                delay(40)
            }
        } else {
            recordingTimecode = "00:00:00:00"
        }
    }

    // Physical Buttons Tactile Controller (replicating iPhone 16 Camera Control)
    var physicalControlMode by remember { mutableStateOf("ZOOM") } // ZOOM, FOCUS, SHUTTER

    LaunchedEffect(Unit) {
        com.example.PhysicalButtonRegistry.events.collect { action ->
            lastActivityTime = System.currentTimeMillis()
            when (action) {
                com.example.PhysicalButtonAction.VOLUME_UP -> {
                    when (physicalControlMode) {
                        "ZOOM" -> {
                            linearZoom = (linearZoom + 0.05f).coerceIn(0f, 1f)
                            activeOverlaySetting = "ZOOM"
                            lastOverlayTriggerTime = System.currentTimeMillis()
                        }
                        "FOCUS" -> {
                            val currentFocus = focusDistance ?: 0f
                            val newFocus = (currentFocus + 0.2f).coerceIn(0f, 10.0f)
                            focusDistance = if (newFocus < 0.1f) null else newFocus
                            activeOverlaySetting = "FOCUS"
                            lastOverlayTriggerTime = System.currentTimeMillis()
                        }
                        "SHUTTER" -> {
                            if (countdownValue == null) {
                                triggerCaptureAction()
                            }
                        }
                    }
                }
                com.example.PhysicalButtonAction.VOLUME_DOWN -> {
                    when (physicalControlMode) {
                        "ZOOM" -> {
                            linearZoom = (linearZoom - 0.05f).coerceIn(0f, 1f)
                            activeOverlaySetting = "ZOOM"
                            lastOverlayTriggerTime = System.currentTimeMillis()
                        }
                        "FOCUS" -> {
                            val currentFocus = focusDistance ?: 0f
                            val newFocus = (currentFocus - 0.2f).coerceIn(0f, 10.0f)
                            focusDistance = if (newFocus < 0.1f) null else newFocus
                            activeOverlaySetting = "FOCUS"
                            lastOverlayTriggerTime = System.currentTimeMillis()
                        }
                        "SHUTTER" -> {
                            if (countdownValue == null) {
                                triggerCaptureAction()
                            }
                        }
                    }
                }
                com.example.PhysicalButtonAction.VOLUME_UP_LONG, com.example.PhysicalButtonAction.VOLUME_DOWN_LONG -> {
                    physicalControlMode = when (physicalControlMode) {
                        "ZOOM" -> "FOCUS"
                        "FOCUS" -> "SHUTTER"
                        else -> "ZOOM"
                    }
                    activeOverlaySetting = "PHYSICAL_$physicalControlMode"
                    lastOverlayTriggerTime = System.currentTimeMillis()
                }
            }
        }
    }

    LaunchedEffect(activeOverlaySetting, lastOverlayTriggerTime) {
        if (activeOverlaySetting != null) {
            kotlinx.coroutines.delay(2000)
            activeOverlaySetting = null
        }
    }
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
    
    val cameraSelector = remember(lensFacing) {
        try {
            CameraSelector.Builder().requireLensFacing(lensFacing).build()
        } catch(e: Exception) {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastActivityTime = System.currentTimeMillis()
                    }
                }
            }
    ) {
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
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = if (lensFacing == CameraSelector.LENS_FACING_FRONT && isFrontCameraMirrored) -1f else 1f
                    },
                    cameraSelector = cameraSelector,
                    imageCapture = imageCapture,
                    videoCapture = videoCapture,
                    isVideoMode = isVideoMode,
                    aspectRatioMode = aspectRatioMode,
                    focusDistance = focusDistance,
                    isExposureLocked = isExposureLocked,
                    exposureCompensation = exposureCompensation,
                    linearZoom = linearZoom,
                    isoValue = isoValue,
                    shutterValue = shutterValue,
                    wbValue = wbValue,
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
                        lastActivityTime = System.currentTimeMillis()
                        exposureCompensation = (exposureCompensation - dy * 0.05f).coerceIn(exposureRangeLower.toFloat(), exposureRangeUpper.toFloat())
                        activeOverlaySetting = "EXPOSURE"
                        lastOverlayTriggerTime = System.currentTimeMillis()
                    },
                    onFocusDistanceChange = { dx ->
                        lastActivityTime = System.currentTimeMillis()
                        val currentFocus = focusDistance ?: 0f
                        val newFocus = (currentFocus - dx * 0.015f).coerceIn(0f, 10f)
                        focusDistance = if (newFocus < 0.1f) null else newFocus
                        activeOverlaySetting = "FOCUS"
                        lastOverlayTriggerTime = System.currentTimeMillis()
                    },
                    onExposureStateAvailable = { lower, upper, step ->
                        exposureRangeLower = lower
                        exposureRangeUpper = upper
                        if (step > 0f) {
                            exposureStep = step
                        }
                    }
                )

                // Viewfinder Sidebar HUD Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .graphicsLayer { alpha = hudAlpha }
                        .then(if (hudAlpha > 0.05f) Modifier else Modifier.clickable(enabled = false) {})
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mirror Preview Toggle (Only available on Front Camera)
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        IconButton(
                            onClick = { isFrontCameraMirrored = !isFrontCameraMirrored },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cameraswitch,
                                contentDescription = "Mirror Preview",
                                tint = if (isFrontCameraMirrored) com.example.ui.theme.Orange500 else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Ghost Overlay Toggle
                    IconButton(
                        onClick = { isGhostOverlayEnabled = !isGhostOverlayEnabled },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Details,
                            contentDescription = "Ghost Mode Overlay",
                            tint = if (isGhostOverlayEnabled) com.example.ui.theme.Orange500 else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Select Custom Ghost Image from Storage
                    IconButton(
                        onClick = { ghostImagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Select Custom Ghost Image",
                            tint = if (ghostOverlayCustomUri != null) com.example.ui.theme.Orange500 else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Clear Custom Ghost Image
                    if (ghostOverlayCustomUri != null) {
                        IconButton(
                            onClick = { 
                                ghostOverlayCustomUri = null
                                ghostOverlaySource = GhostOverlaySource.LATEST_CAPTURE
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear Custom Ghost Image",
                                tint = com.example.ui.theme.Red500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Ghost Opacity Selector (Expanded only when Ghost is active)
                    if (isGhostOverlayEnabled) {
                        Box(modifier = Modifier.width(20.dp).height(1.dp).background(Color.White.copy(alpha = 0.3f)))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("OPAC", color = Color.Gray, fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            listOf(0.2f, 0.4f, 0.6f, 0.8f).forEach { opacity ->
                                val isSelected = Math.abs(ghostOverlayOpacity - opacity) < 0.05f
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) com.example.ui.theme.Orange500 else Color.Transparent)
                                        .clickable { ghostOverlayOpacity = opacity },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${(opacity * 100).toInt()}",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Tactile Controls Mode Quick Selector
                    Box(modifier = Modifier.width(20.dp).height(1.dp).background(Color.White.copy(alpha = 0.3f)))
                    IconButton(
                        onClick = {
                            physicalControlMode = when (physicalControlMode) {
                                "ZOOM" -> "FOCUS"
                                "FOCUS" -> "SHUTTER"
                                else -> "ZOOM"
                            }
                            activeOverlaySetting = "PHYSICAL_$physicalControlMode"
                            lastOverlayTriggerTime = System.currentTimeMillis()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = when(physicalControlMode) {
                                "ZOOM" -> Icons.Filled.ZoomIn
                                "FOCUS" -> Icons.Filled.FilterCenterFocus
                                else -> Icons.Filled.PhotoCamera
                            },
                            contentDescription = "Physical Control Mode",
                            tint = com.example.ui.theme.Orange500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Ghost Overlay Layer (vlog frame matching)
                if (isGhostOverlayEnabled) {
                    val ghostUri = ghostOverlayCustomUri ?: latestMediaItem?.uri
                    if (ghostUri != null) {
                        coil.compose.AsyncImage(
                            model = ghostUri,
                            contentDescription = "Ghost Overlay",
                            alpha = ghostOverlayOpacity,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.matchParentSize().graphicsLayer {
                                scaleX = if (lensFacing == CameraSelector.LENS_FACING_FRONT && isFrontCameraMirrored) -1f else 1f
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Ghost Mode Active\nSelect an image from storage or take a photo to use as overlay",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Virtual Horizon inside active viewport
                if (isOverlaysEnabled && isVirtualHorizonEnabled) {
                    VirtualHorizon(modifier = Modifier.matchParentSize())
                }

                // Composition Overlays inside active viewport (without risk of being covered)
                androidx.compose.animation.Crossfade(
                    targetState = if (isOverlaysEnabled) compositionOverlay else CompositionOverlay.NONE,
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

                // Contextual Gesture HUD Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeOverlaySetting != null,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (activeOverlaySetting == "EXPOSURE") {
                                Icon(
                                    imageVector = Icons.Filled.Exposure,
                                    contentDescription = "Exposure compensation",
                                    tint = com.example.ui.theme.Orange500,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = String.format(Locale.US, "EV %+.1f", exposureCompensation),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Swipe Vertically to Adjust",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            } else if (activeOverlaySetting == "FOCUS") {
                                Icon(
                                    imageVector = Icons.Filled.FilterCenterFocus,
                                    contentDescription = "Focus distance",
                                    tint = com.example.ui.theme.Orange500,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val isAuto = focusDistance == null
                                Text(
                                    text = if (isAuto) "FOCUS: AUTO (AF)" else String.format(Locale.US, "FOCUS: MF (%.2f)", focusDistance),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Swipe Horizontally to Adjust",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            } else if (activeOverlaySetting?.startsWith("PHYSICAL_") == true) {
                                val mode = activeOverlaySetting?.removePrefix("PHYSICAL_") ?: "ZOOM"
                                Icon(
                                    imageVector = when(mode) {
                                        "ZOOM" -> Icons.Filled.ZoomIn
                                        "FOCUS" -> Icons.Filled.FilterCenterFocus
                                        else -> Icons.Filled.PhotoCamera
                                    },
                                    contentDescription = "Physical Mode",
                                    tint = com.example.ui.theme.Orange500,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "PHYSICAL CONTROL: $mode",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when(mode) {
                                        "ZOOM" -> "VOL +: ZOOM IN | VOL -: ZOOM OUT"
                                        "FOCUS" -> "VOL +: FOCUS IN | VOL -: FOCUS OUT"
                                        else -> "VOL +/-: SHUTTER (CAPTURE/RECORD)"
                                    },
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            } else if (activeOverlaySetting == "ZOOM") {
                                Icon(
                                    imageVector = Icons.Filled.ZoomIn,
                                    contentDescription = "Zoom level",
                                    tint = com.example.ui.theme.Orange500,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = String.format(Locale.US, "ZOOM: %.2fx", 1f + linearZoom * 9f),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "VOL +/- or Dial to Adjust",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Stabilization active text centered at the bottom of active viewport
                if (isOverlaysEnabled) {
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
                }

                if (isOverlaysEnabled && zebraStripes) {
                    ZebraStripesOverlay(modifier = Modifier.matchParentSize())
                }

                if (isBurstCapturing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = burstProgress.toFloat() / burstTotal.toFloat(),
                                color = com.example.ui.theme.Orange500
                            )
                            Text(
                                text = "CAPTURING BURST",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "$burstProgress / $burstTotal (${if (burstType.contains("RAW")) "RAW+JPEG" else "JPEG"})",
                                color = com.example.ui.theme.Orange500,
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .graphicsLayer { alpha = hudAlpha }
                    .then(if (hudAlpha > 0.05f) Modifier else Modifier.clickable(enabled = false) {})
                    .clickable {
                        inputSceneName = currentScene
                        isSlateDialogExpanded = true
                    }
            ) {
                Text("SLATE (SCENE/TAKE)", color = com.example.ui.theme.Neutral500, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 2.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("S: $currentScene | T: $currentTake", color = com.example.ui.theme.Orange500, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Slate", tint = com.example.ui.theme.Orange500, modifier = Modifier.size(12.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Essential REC TIME: Always visible (1.0f)
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                    Text("REC TIME", color = com.example.ui.theme.Neutral500, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 2.sp)
                    Text(recordingTimecode, color = if (isRecording) com.example.ui.theme.Red500 else Color.White, fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                
                // Non-essential controls: Fades out
                Row(
                    modifier = Modifier
                        .graphicsLayer { alpha = hudAlpha }
                        .then(if (hudAlpha > 0.05f) Modifier else Modifier.clickable(enabled = false) {})
                        .padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { isOverlaysEnabled = !isOverlaysEnabled }
                    ) {
                        Icon(
                            imageVector = if (isOverlaysEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Overlays",
                            tint = if (isOverlaysEnabled) com.example.ui.theme.Orange500 else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("OVERLAYS", color = if (isOverlaysEnabled) com.example.ui.theme.Orange500 else Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                onAudioSourceChange = { audioSource = it },
                burstModeEnabled = burstModeEnabled,
                onBurstModeEnabledChange = { burstModeEnabled = it },
                burstType = burstType,
                onBurstTypeChange = { burstType = it }
            )
        }
        
        // Footer (Unified Creative Control Deck)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp)
        ) {
            // 1. Collapsible Camera Control Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = hudAlpha }
                    .then(if (hudAlpha > 0.05f) Modifier else Modifier.clickable(enabled = false) {})
            ) {
                CameraControlsStrip(
                    compositionOverlay = compositionOverlay,
                    onOverlayChange = { 
                        compositionOverlay = it
                        lastActivityTime = System.currentTimeMillis()
                    },
                    aspectRatioMode = aspectRatioMode,
                    onAspectRatioChange = { 
                        aspectRatioMode = it
                        lastActivityTime = System.currentTimeMillis()
                    },
                    isVirtualHorizonEnabled = isVirtualHorizonEnabled,
                    onHorizonToggle = { 
                        isVirtualHorizonEnabled = !isVirtualHorizonEnabled
                        lastActivityTime = System.currentTimeMillis()
                    },
                    isoValue = isoValue,
                    onIsoChange = { 
                        isoValue = it
                        lastActivityTime = System.currentTimeMillis()
                    },
                    shutterValue = shutterValue,
                    onShutterChange = { 
                        shutterValue = it
                        lastActivityTime = System.currentTimeMillis()
                    },
                    wbValue = wbValue,
                    onWbChange = { 
                        wbValue = it
                        lastActivityTime = System.currentTimeMillis()
                    },
                    focusDistance = focusDistance,
                    onFocusChange = { 
                        focusDistance = it
                        lastActivityTime = System.currentTimeMillis()
                    },
                    activeSubPanel = activeSubPanel,
                    onActiveSubPanelChange = { 
                        activeSubPanel = it
                        lastActivityTime = System.currentTimeMillis()
                    },
                    isoInteractionSource = isoInteractionSource,
                    shutterInteractionSource = shutterInteractionSource,
                    wbInteractionSource = wbInteractionSource,
                    focusInteractionSource = focusInteractionSource,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 2. iPhone-Style Tactile Zoom Ruler & Pills
            ZoomDial(
                linearZoom = linearZoom,
                onZoomChange = { 
                    linearZoom = it
                    lastActivityTime = System.currentTimeMillis()
                },
                onDraggingStateChange = { isZoomDialDragging = it },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .graphicsLayer { alpha = hudAlpha }
                    .then(if (hudAlpha > 0.05f) Modifier else Modifier.clickable(enabled = false) {})
            )

            // 3. Mode Switcher (PHOTO / VIDEO)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = hudAlpha }
                    .then(if (hudAlpha > 0.05f) Modifier else Modifier.clickable(enabled = false) {})
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PHOTO",
                    color = if (!isVideoMode) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .clickable { if (!isRecording) isVideoMode = false }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
                Text(
                    "VIDEO",
                    color = if (isVideoMode) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .clickable { if (!isRecording) isVideoMode = true }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // 4. Primary Capture Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Thumbnail Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { alpha = hudAlpha }
                        .then(if (hudAlpha > 0.05f) Modifier else Modifier.clickable(enabled = false) {})
                        .clip(CircleShape)
                        .background(com.example.ui.theme.Neutral800)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("gallery_button")
                        .clickable { isGalleryOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    val currentLatestItem = latestMediaItem
                    if (currentLatestItem != null) {
                        coil.compose.AsyncImage(
                            model = currentLatestItem.uri,
                            contentDescription = "Gallery Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.Image, contentDescription = "Gallery", tint = com.example.ui.theme.Neutral500)
                    }
                }

                // Main Shutter: Always visible (essential)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (countdownValue != null) return@clickable // Ignore clicks during countdown
                            
                            val startAction = {
                                triggerCaptureAction()
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
                    val glowColor = if (isVideoMode) com.example.ui.theme.Red500.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f)
                    Box(modifier = Modifier.size(76.dp).clip(CircleShape).background(glowColor))
                    // White border
                    Box(modifier = Modifier.size(62.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        // Inner Black Gap
                        Box(modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                            // Core
                            Box(
                                modifier = Modifier
                                    .size(if (isRecording) 24.dp else 50.dp)
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
                        .graphicsLayer { alpha = hudAlpha }
                        .then(if (hudAlpha > 0.05f) Modifier else Modifier.clickable(enabled = false) {})
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.FlipCameraIos, contentDescription = "Flip", tint = Color.White)
                }
            }
        }
    }

    if (isGalleryOpen) {
        MediaGalleryScreen(onClose = { isGalleryOpen = false })
    }

    if (isSlateDialogExpanded) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { isSlateDialogExpanded = false },
            title = {
                Text(
                    text = "Edit Slate (Scene/Take Metadata)",
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Customize current scene name and take number to automatically tag recorded video and image clips.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = inputSceneName,
                        onValueChange = { inputSceneName = it },
                        label = { Text("Scene ID / Name") },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = com.example.ui.theme.Orange500,
                            unfocusedLabelColor = Color.Gray,
                            focusedIndicatorColor = com.example.ui.theme.Orange500,
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Take Number", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.FilledIconButton(
                                onClick = { if (currentTake > 1) currentTake -= 1 },
                                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-", color = Color.White, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                            Text(
                                text = "$currentTake",
                                color = com.example.ui.theme.Orange500,
                                fontSize = 18.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            androidx.compose.material3.FilledIconButton(
                                onClick = { currentTake += 1 },
                                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+", color = Color.White, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        currentScene = inputSceneName.ifBlank { "01" }
                        isSlateDialogExpanded = false
                    }
                ) {
                    Text("Apply Slate", color = com.example.ui.theme.Orange500, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { isSlateDialogExpanded = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    scene: String,
    take: Int,
    onComplete: (Boolean) -> Unit
) {
    val uniqueId = java.util.UUID.randomUUID().toString().take(4)
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val name = "VLOG_S${scene}_T${take}_${timestamp}_$uniqueId"
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
                onComplete(false)
            }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val msg = "Photo capture succeeded: ${output.savedUri}"
                Toast.makeText(context, "Saved Photo: S${scene} T${take}", Toast.LENGTH_LONG).show()
                Log.d("CameraScreen", msg)
                onComplete(true)
            }
        }
    )
}

private fun takeBurst(
    context: Context,
    imageCapture: ImageCapture,
    scene: String,
    startTake: Int,
    burstType: String,
    onProgress: (Int, Int) -> Unit,
    onComplete: (Int) -> Unit
) {
    val totalFrames = if (burstType.contains("RAW")) 5 else 10
    val delayMs = if (burstType.contains("RAW")) 330L else 100L
    
    val mainExecutor = ContextCompat.getMainExecutor(context)
    val executorService = java.util.concurrent.Executors.newFixedThreadPool(4)
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    
    scope.launch {
        var capturedCount = 0
        for (i in 1..totalFrames) {
            val frameTake = startTake + i - 1
            val uniqueId = java.util.UUID.randomUUID().toString().take(4)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US).format(System.currentTimeMillis())
            val baseName = "VLOG_S${scene}_T${frameTake}_${timestamp}_$uniqueId"
            
            val jpegValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, baseName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }
            val jpegOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                jpegValues
            ).build()
            
            val success = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { continuation ->
                imageCapture.takePicture(
                    jpegOptions,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e("CameraScreen", "Burst Frame $i failed: ${exc.message}", exc)
                            if (continuation.isActive) continuation.resume(true) {}
                        }
                        
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            executorService.execute {
                                if (burstType.contains("RAW")) {
                                    try {
                                        val rawValues = android.content.ContentValues().apply {
                                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "${baseName}_RAW")
                                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/x-adobe-dng")
                                        }
                                        val rawUri = context.contentResolver.insert(
                                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            rawValues
                                        )
                                        rawUri?.let { uri ->
                                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                                val simulatedRawData = "DNG_MAGIC_RAW_DATA_S${scene}_T${frameTake}_${System.currentTimeMillis()}".toByteArray()
                                                stream.write(simulatedRawData)
                                            }
                                        }
                                        Log.d("CameraScreen", "Saved RAW companion for frame $i")
                                    } catch (e: Exception) {
                                        Log.e("CameraScreen", "Failed to save simulated RAW companion", e)
                                    }
                                }
                            }
                            if (continuation.isActive) continuation.resume(true) {}
                        }
                    }
                )
            }
            
            if (success) {
                capturedCount++
                onProgress(capturedCount, totalFrames)
            }
            
            kotlinx.coroutines.delay(delayMs)
        }
        
        executorService.shutdown()
        onComplete(capturedCount)
    }
}

private fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    scene: String,
    take: Int,
    onRecordingStarted: (Recording) -> Unit,
    onVideoSaved: () -> Unit
) {
    val uniqueId = java.util.UUID.randomUUID().toString().take(4)
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val name = "VLOG_S${scene}_T${take}_${timestamp}_$uniqueId"
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
                            Toast.makeText(context, "Saved Video: S${scene} T${take}", Toast.LENGTH_LONG).show()
                            Log.d("CameraScreen", msg)
                            onVideoSaved()
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

@Composable
fun ZoomDial(
    linearZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onDraggingStateChange: (Boolean) -> Unit = {}
) {
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(isDragging) {
        onDraggingStateChange(isDragging)
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zoom Value Badge
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%.1fx", 1.0f + linearZoom * 7.0f),
                color = com.example.ui.theme.Orange500,
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Ticks Ruler Canvas with Drag Gesture (iPhone style slider wheel dial)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            // Drag sensitivity: 400 pixels across the screen is full range
                            val sensitivity = 400f
                            val newZoom = (linearZoom + dragAmount / sensitivity).coerceIn(0f, 1f)
                            onZoomChange(newZoom)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerX = width / 2
                
                val tickCount = 40
                val tickSpacing = 16.dp.toPx()
                val currentOffset = linearZoom * (tickCount * tickSpacing)
                
                for (i in 0..tickCount) {
                    val tickZoomValue = i.toFloat() / tickCount
                    val tickOffset = i * tickSpacing - currentOffset
                    val x = centerX + tickOffset
                    
                    if (x >= 0 && x <= width) {
                        val isMajor = i % 5 == 0
                        val isSemiMajor = i % 5 != 0 && i % 2 == 0
                        
                        val tickHeight = when {
                            isMajor -> 20.dp.toPx()
                            isSemiMajor -> 12.dp.toPx()
                            else -> 8.dp.toPx()
                        }
                        
                        val tickAlpha = when {
                            x < centerX -> (x / centerX).coerceIn(0f, 1f)
                            else -> ((width - x) / (width - centerX)).coerceIn(0f, 1f)
                        }
                        
                        val tickColor = if (isMajor) Color.White else Color.White.copy(alpha = 0.4f)
                        
                        drawLine(
                            color = tickColor.copy(alpha = tickAlpha),
                            start = androidx.compose.ui.geometry.Offset(x, height - tickHeight),
                            end = androidx.compose.ui.geometry.Offset(x, height),
                            strokeWidth = if (isMajor) 2f else 1f
                        )
                    }
                }
                
                // Central Fixed Orange Indicator Line
                drawLine(
                    color = com.example.ui.theme.Orange500,
                    start = androidx.compose.ui.geometry.Offset(centerX, height - 28.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(centerX, height),
                    strokeWidth = 3f
                )
            }
        }

        // Quick Jump Pills Row (iPhone Style Shortcuts)
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val levels = listOf(
                "1x" to 0f,
                "2x" to 1f / 7f,
                "5x" to 4f / 7f,
                "8x" to 1f
            )
            
            levels.forEach { (label, value) ->
                val isSelected = kotlin.math.abs(linearZoom - value) < 0.05f
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) com.example.ui.theme.Orange500 
                            else Color.Black.copy(alpha = 0.5f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .clickable { onZoomChange(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun CameraControlsStrip(
    compositionOverlay: CompositionOverlay,
    onOverlayChange: (CompositionOverlay) -> Unit,
    aspectRatioMode: AspectRatioMode,
    onAspectRatioChange: (AspectRatioMode) -> Unit,
    isVirtualHorizonEnabled: Boolean,
    onHorizonToggle: () -> Unit,
    isoValue: Float,
    onIsoChange: (Float) -> Unit,
    shutterValue: Float,
    onShutterChange: (Float) -> Unit,
    wbValue: Float,
    onWbChange: (Float) -> Unit,
    focusDistance: Float?,
    onFocusChange: (Float?) -> Unit,
    activeSubPanel: UtilitySubPanel,
    onActiveSubPanelChange: (UtilitySubPanel) -> Unit,
    isoInteractionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    shutterInteractionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    wbInteractionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    focusInteractionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    var activeManualParam by remember { mutableStateOf("ISO") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Expanded Panel Content
        androidx.compose.animation.AnimatedContent(
            targetState = activeSubPanel,
            transitionSpec = {
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) with
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
            },
            label = "subPanelTransition"
        ) { panel ->
            when (panel) {
                UtilitySubPanel.NONE -> {
                    // Minimalist default strip
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UtilityIconButton(
                            icon = Icons.Filled.Grid3x3,
                            label = "GRID",
                            isActive = compositionOverlay != CompositionOverlay.NONE,
                            onClick = { onActiveSubPanelChange(UtilitySubPanel.GRID) }
                        )

                        UtilityIconButton(
                            icon = Icons.Filled.CropSquare,
                            label = "ASPECT",
                            isActive = false,
                            onClick = { onActiveSubPanelChange(UtilitySubPanel.ASPECT) }
                        )

                        UtilityIconButton(
                            icon = Icons.Filled.Tune,
                            label = "MANUAL",
                            isActive = focusDistance != null || isoValue != 400f || shutterValue != 125f || wbValue != 5600f,
                            onClick = { onActiveSubPanelChange(UtilitySubPanel.MANUAL) }
                        )

                        UtilityIconButton(
                            icon = Icons.Filled.ScreenRotation,
                            label = "LEVEL",
                            isActive = isVirtualHorizonEnabled,
                            onClick = onHorizonToggle
                        )
                    }
                }

                UtilitySubPanel.GRID -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { onActiveSubPanelChange(UtilitySubPanel.NONE) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val grids = listOf(
                                "OFF" to CompositionOverlay.NONE,
                                "3x3" to CompositionOverlay.RULE_OF_THIRDS,
                                "PHI" to CompositionOverlay.GOLDEN_RATIO,
                                "SPIRAL" to CompositionOverlay.GOLDEN_SPIRAL,
                                "TRI" to CompositionOverlay.GOLDEN_TRIANGLE,
                                "SYM" to CompositionOverlay.DYNAMIC_SYMMETRY
                            )

                            grids.forEach { (label, overlay) ->
                                val isSelected = compositionOverlay == overlay
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) com.example.ui.theme.Orange500 else Color.Transparent)
                                        .clickable { onOverlayChange(overlay) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                UtilitySubPanel.ASPECT -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { onActiveSubPanelChange(UtilitySubPanel.NONE) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
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
                                        .clickable { onAspectRatioChange(mode) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                UtilitySubPanel.MANUAL -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { onActiveSubPanelChange(UtilitySubPanel.NONE) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("ISO", "SHUTTER", "WB", "FOCUS").forEach { param ->
                                    val isSelected = activeManualParam == param
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                            .clickable { activeManualParam = param }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = param,
                                            color = if (isSelected) com.example.ui.theme.Orange500 else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            when (activeManualParam) {
                                "ISO" -> {
                                    Text(
                                        text = "ISO: ${isoValue.toInt()}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Slider(
                                        value = isoValue,
                                        onValueChange = onIsoChange,
                                        valueRange = 100f..3200f,
                                        interactionSource = isoInteractionSource,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = com.example.ui.theme.Orange500
                                        )
                                    )
                                }
                                "SHUTTER" -> {
                                    Text(
                                        text = "SHUTTER: 1/${shutterValue.toInt()}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Slider(
                                        value = shutterValue,
                                        onValueChange = onShutterChange,
                                        valueRange = 30f..1000f,
                                        interactionSource = shutterInteractionSource,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = com.example.ui.theme.Orange500
                                        )
                                    )
                                }
                                "WB" -> {
                                    Text(
                                        text = "WB: ${wbValue.toInt()}K",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Slider(
                                        value = wbValue,
                                        onValueChange = onWbChange,
                                        valueRange = 2000f..8000f,
                                        interactionSource = wbInteractionSource,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = com.example.ui.theme.Orange500
                                        )
                                    )
                                }
                                "FOCUS" -> {
                                    val isAuto = focusDistance == null
                                    Text(
                                        text = "FOCUS: ${if (isAuto) "AUTO (AF)" else String.format(Locale.US, "MF (%.2f)", focusDistance)}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Slider(
                                        value = focusDistance ?: 0f,
                                        onValueChange = { onFocusChange(if (it < 0.1f) null else it) },
                                        valueRange = 0f..10f,
                                        interactionSource = focusInteractionSource,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = com.example.ui.theme.Orange500
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UtilityIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) com.example.ui.theme.Orange500 else Color.White.copy(alpha = 0.5f),
            fontSize = 8.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
