import re

content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()

target = """    isExposureLocked: Boolean = false,
    onTapToFocus: (Float, Float) -> Unit = { _, _ -> },
    onLongPressToLock: (Float, Float) -> Unit = { _, _ -> }
) {"""

replacement = """    isExposureLocked: Boolean = false,
    exposureCompensation: Float = 0f,
    onTapToFocus: (Float, Float) -> Unit = { _, _ -> },
    onLongPressToLock: (Float, Float) -> Unit = { _, _ -> },
    onExposureChange: (Float) -> Unit = {}
) {"""

content = content.replace(target, replacement)

# Add onScroll to GestureDetector
target_gesture = """                    override fun onDown(e: android.view.MotionEvent): Boolean {
                        return true
                    }
                })"""

replacement_gesture = """                    override fun onDown(e: android.view.MotionEvent): Boolean {
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
                })"""

content = content.replace(target_gesture, replacement_gesture)

# Also update the LaunchedEffect to apply exposure compensation
target_effect = """            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, isExposureLocked)
            camera2CameraControl.captureRequestOptions = builder.build()"""

replacement_effect = """            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, isExposureLocked)
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
"""

content = content.replace(target_effect, replacement_effect)

# Update LaunchedEffect dependency
content = content.replace("LaunchedEffect(focusDistance, isExposureLocked, currentCamera)", "LaunchedEffect(focusDistance, isExposureLocked, exposureCompensation, currentCamera)")

open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
print("Updated CameraPreview")
