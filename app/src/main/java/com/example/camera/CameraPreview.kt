package com.example.camera

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.coroutines.resume
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import android.hardware.Camera
import android.util.Size
import kotlin.math.ln
import kotlin.math.pow

@Suppress("DEPRECATION")
private fun getLegacyCameraId(lensFacing: Int): Int {
    val cameraInfo = Camera.CameraInfo()
    for (i in 0 until Camera.getNumberOfCameras()) {
        Camera.getCameraInfo(i, cameraInfo)
        val expectedFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            Camera.CameraInfo.CAMERA_FACING_BACK
        } else {
            Camera.CameraInfo.CAMERA_FACING_FRONT
        }
        if (cameraInfo.facing == expectedFacing) {
            return i
        }
    }
    return 0
}

@Suppress("DEPRECATION")
private fun getOptimalPreviewSize(lensFacing: Int, targetWidth: Int, targetHeight: Int): Camera.Size? {
    var camera: Camera? = null
    try {
        val cameraId = getLegacyCameraId(lensFacing)
        camera = Camera.open(cameraId)
        val parameters = camera.parameters
        val supportedSizes = parameters.supportedPreviewSizes // Always iterate through Camera.Parameters.getSupportedPreviewSizes()
        if (supportedSizes.isNullOrEmpty()) return null
        
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
        var optimalSize: Camera.Size? = null
        var minDiff = Float.MAX_VALUE
        
        for (size in supportedSizes) {
            val ratio = size.width.toFloat() / size.height.toFloat()
            val diff = kotlin.math.abs(ratio - targetRatio)
            if (diff < minDiff) {
                optimalSize = size
                minDiff = diff
            } else if (diff == minDiff) {
                if (optimalSize == null || size.width * size.height > optimalSize.width * optimalSize.height) {
                    optimalSize = size
                }
            }
        }
        return optimalSize
    } catch (e: Exception) {
        Log.e("CameraPreview", "Failed to get legacy supported preview sizes", e)
        return null
    } finally {
        try {
            camera?.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}

@Suppress("DEPRECATION")
private fun getRggbGainsForKelvin(kelvin: Float): android.hardware.camera2.params.RggbChannelVector {
    val temp = (kelvin / 100.0f).coerceIn(10f, 400f)
    var r: Float
    var g: Float
    var b: Float

    if (temp <= 66.0f) {
        r = 255.0f
        g = temp
        g = (99.4708025861 * ln(g.toDouble()) - 161.1195681661).toFloat()
        if (temp <= 19.0f) {
            b = 0.0f
        } else {
            b = temp - 10.0f
            b = (138.5177312231 * ln(b.toDouble()) - 305.0447927307).toFloat()
        }
    } else {
        r = temp - 60.0f
        r = (329.698727446 * r.toDouble().pow(-0.1332047592)).toFloat()
        g = temp - 60.0f
        g = (288.1221695283 * g.toDouble().pow(-0.0755148492)).toFloat()
        b = 255.0f
    }

    r = r.coerceIn(0f, 255f)
    g = g.coerceIn(0f, 255f)
    b = b.coerceIn(0f, 255f)

    val rGain = (255.0f / r).coerceIn(0.5f, 4.0f)
    val gGain = (255.0f / g).coerceIn(0.5f, 4.0f)
    val bGain = (255.0f / b).coerceIn(0.5f, 4.0f)

    return android.hardware.camera2.params.RggbChannelVector(rGain, gGain, gGain, bGain)
}

@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
@Composable
fun CameraPreviewWithUseCases(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    imageCapture: ImageCapture,
    videoCapture: VideoCapture<Recorder>,
    isVideoMode: Boolean = false,
    aspectRatioMode: AspectRatioMode = AspectRatioMode.RATIO_9_16,
    focusDistance: Float? = null,
    isExposureLocked: Boolean = false,
    exposureCompensation: Float = 0f,
    linearZoom: Float = 0f,
    isoValue: Float = 400f,
    shutterValue: Float = 125f,
    wbValue: Float = 5600f,
    onTapToFocus: (Float, Float) -> Unit = { _, _ -> },
    onLongPressToLock: (Float, Float) -> Unit = { _, _ -> },
    onExposureChange: (Float) -> Unit = {},
    onFocusDistanceChange: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var currentCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    LaunchedEffect(linearZoom, currentCamera) {
        val camera = currentCamera ?: return@LaunchedEffect
        try {
            camera.cameraControl.setLinearZoom(linearZoom)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Failed to set zoom to $linearZoom", e)
        }
    }

    LaunchedEffect(focusDistance, isExposureLocked, exposureCompensation, isoValue, shutterValue, wbValue, currentCamera) {
        val camera = currentCamera ?: return@LaunchedEffect
        try {
            val camera2CameraControl = Camera2CameraControl.from(camera.cameraControl)
            val builder = CaptureRequestOptions.Builder()
            
            // 1. Manual Focus Control
            if (focusDistance != null) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)
            } else {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
            
            // 2. Manual Exposure Control (ISO & Shutter Speed)
            // If user adjusted ISO or Shutter Speed from their defaults, or exposure is locked, turn AE off and set values manually.
            val isManualExposure = isExposureLocked || isoValue != 400f || shutterValue != 125f
            if (isManualExposure) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoValue.toInt())
                val expTimeNs = (1_000_000_000L / shutterValue.toDouble()).toLong()
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, expTimeNs)
            } else {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, false)
            }

            // 3. Manual White Balance (WB) Control
            // Set AWB to OFF if user overrides WB, and apply calculated color gains.
            if (wbValue != 5600f) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
                builder.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
                val gains = getRggbGainsForKelvin(wbValue)
                builder.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
            } else {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO)
            }

            camera2CameraControl.captureRequestOptions = builder.build()
            
            // Apply exposure compensation via standard CameraX API
            val range = camera.cameraInfo.exposureState.exposureCompensationRange
            if (range.contains(exposureCompensation.toInt())) {
                camera.cameraControl.setExposureCompensationIndex(exposureCompensation.toInt())
            } else if (exposureCompensation > range.upper) {
                camera.cameraControl.setExposureCompensationIndex(range.upper)
            } else if (exposureCompensation < range.lower) {
                camera.cameraControl.setExposureCompensationIndex(range.lower)
            }

        } catch (e: Exception) {
            Log.e("CameraPreview", "Failed to set camera controls", e)
        }
    }

    LaunchedEffect(cameraSelector, isVideoMode, imageCapture, videoCapture, aspectRatioMode) {
        val provider = kotlinx.coroutines.suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(context))
        }
        cameraProvider = provider

        val cxRatio = when (aspectRatioMode) {
            AspectRatioMode.RATIO_16_9, AspectRatioMode.RATIO_9_16 -> androidx.camera.core.AspectRatio.RATIO_16_9
            AspectRatioMode.RATIO_4_3, AspectRatioMode.RATIO_3_4 -> androidx.camera.core.AspectRatio.RATIO_4_3
            else -> androidx.camera.core.AspectRatio.RATIO_16_9
        }

        val targetWidth = when (aspectRatioMode) {
            AspectRatioMode.RATIO_9_16, AspectRatioMode.RATIO_16_9 -> 1920
            AspectRatioMode.RATIO_3_4, AspectRatioMode.RATIO_4_3 -> 1440
            AspectRatioMode.RATIO_1_1 -> 1080
            else -> 1920
        }
        val targetHeight = when (aspectRatioMode) {
            AspectRatioMode.RATIO_9_16, AspectRatioMode.RATIO_16_9 -> 1080
            AspectRatioMode.RATIO_3_4, AspectRatioMode.RATIO_4_3 -> 1080
            AspectRatioMode.RATIO_1_1 -> 1080
            else -> 1080
        }

        val targetLensFacing = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        val optimalSize = getOptimalPreviewSize(targetLensFacing, targetWidth, targetHeight)

        val previewBuilder = Preview.Builder()
        if (optimalSize != null) {
            Log.d("CameraPreview", "Setting optimal preview size dynamically: ${optimalSize.width}x${optimalSize.height}")
            @Suppress("DEPRECATION")
            previewBuilder.setTargetResolution(Size(optimalSize.width, optimalSize.height))
        } else {
            previewBuilder.setTargetAspectRatio(cxRatio)
        }

        val preview = previewBuilder.build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }
        try {
            provider.unbindAll()
            
            var resolvedSelector = cameraSelector
            if (!provider.hasCamera(resolvedSelector)) {
                resolvedSelector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    Log.e("CameraPreview", "No camera available on device")
                    return@LaunchedEffect
                }
            }

            val dm = context.resources.displayMetrics

            val rational = when (aspectRatioMode) {
                AspectRatioMode.RATIO_9_16 -> android.util.Rational(9, 16)
                AspectRatioMode.RATIO_3_4 -> android.util.Rational(3, 4)
                AspectRatioMode.RATIO_1_1 -> android.util.Rational(1, 1)
                AspectRatioMode.RATIO_4_3 -> android.util.Rational(4, 3)
                AspectRatioMode.RATIO_16_9 -> android.util.Rational(16, 9)
                AspectRatioMode.FULL -> {
                    android.util.Rational(dm.widthPixels, dm.heightPixels)
                }
            }

            val displayRotation = try {
                previewView.display?.rotation ?: android.view.Surface.ROTATION_0
            } catch (e: Exception) {
                android.view.Surface.ROTATION_0
            }

            val viewPort = androidx.camera.core.ViewPort.Builder(rational, displayRotation)
                .setScaleType(androidx.camera.core.ViewPort.FILL_CENTER)
                .build()

            val useCaseGroup = androidx.camera.core.UseCaseGroup.Builder()
                .addUseCase(preview)
                .setViewPort(viewPort)
                .apply {
                    if (isVideoMode) {
                        addUseCase(videoCapture)
                    } else {
                        addUseCase(imageCapture)
                    }
                }
                .build()

            currentCamera = provider.bindToLifecycle(
                lifecycleOwner,
                resolvedSelector,
                useCaseGroup
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            // CameraX automatically unbinds when the lifecycle owner is destroyed.
            // Removing manual unbindAll() to prevent AppOps 'Operation not started' false-positives.
        }
    }

    AndroidView(
        factory = {
            previewView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(event: android.view.MotionEvent): Boolean {
                        val factory = meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                        currentCamera?.cameraControl?.startFocusAndMetering(action)
                        onTapToFocus(event.x, event.y)
                        return true
                    }
                    override fun onLongPress(event: android.view.MotionEvent) {
                        val factory = meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                        currentCamera?.cameraControl?.startFocusAndMetering(action)
                        onLongPressToLock(event.x, event.y)
                    }
                    override fun onDown(e: android.view.MotionEvent): Boolean {
                        return true
                    }
                    override fun onScroll(
                        e1: android.view.MotionEvent?,
                        e2: android.view.MotionEvent,
                        distanceX: Float,
                        distanceY: Float
                    ): Boolean {
                        if (kotlin.math.abs(distanceX) > kotlin.math.abs(distanceY)) {
                            onFocusDistanceChange(distanceX)
                        } else {
                            onExposureChange(distanceY)
                        }
                        return true
                    }
                })
                setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    true
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
