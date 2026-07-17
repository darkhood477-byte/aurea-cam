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

@Composable
fun CameraPreviewWithUseCases(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    imageCapture: ImageCapture,
    videoCapture: VideoCapture<Recorder>,
    isVideoMode: Boolean = false,
    focusDistance: Float? = null,
    isExposureLocked: Boolean = false,
    exposureCompensation: Float = 0f,
    onTapToFocus: (Float, Float) -> Unit = { _, _ -> },
    onLongPressToLock: (Float, Float) -> Unit = { _, _ -> },
    onExposureChange: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var currentCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    LaunchedEffect(focusDistance, isExposureLocked, exposureCompensation, currentCamera) {
        val camera = currentCamera ?: return@LaunchedEffect
        try {
            val camera2CameraControl = Camera2CameraControl.from(camera.cameraControl)
            val builder = CaptureRequestOptions.Builder()
            if (focusDistance != null) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)
            } else {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, isExposureLocked)
            camera2CameraControl.captureRequestOptions = builder.build()
            
            // Apply exposure compensation via standard CameraX API
            // Usually ranges from -12 to +12, we can just pass an int
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

    LaunchedEffect(cameraSelector) {
        val provider = kotlinx.coroutines.suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(context))
        }
        cameraProvider = provider
        val preview = Preview.Builder().build().also {
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

            currentCamera = provider.bindToLifecycle(
                lifecycleOwner,
                resolvedSelector,
                preview,
                imageCapture,
                videoCapture
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            // cameraProvider?.unbindAll()
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
                        onExposureChange(distanceY)
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
