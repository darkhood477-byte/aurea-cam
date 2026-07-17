import re

content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()

target = """    LaunchedEffect(cameraSelector, isVideoMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
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
                        // Return early if no camera exists
                        Log.e("CameraPreview", "No camera available on device")
                        return@addListener
                    }
                }

                if (isVideoMode) {
                    currentCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        resolvedSelector,
                        preview,
                        videoCapture
                    )
                } else {
                    currentCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        resolvedSelector,
                        preview,
                        imageCapture
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraPreview", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }"""

replacement = """    LaunchedEffect(cameraSelector, isVideoMode) {
        val provider = kotlinx.coroutines.suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                continuation.resume(future.get(), null)
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

            if (isVideoMode) {
                currentCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    resolvedSelector,
                    preview,
                    videoCapture
                )
            } else {
                currentCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    resolvedSelector,
                    preview,
                    imageCapture
                )
            }
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
    print("Fixed LaunchedEffect")
else:
    print("Not found target")
