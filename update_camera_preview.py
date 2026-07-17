import re

content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()

content = content.replace(
"""    isExposureLocked: Boolean = false,
    onTapToFocus: (Float, Float) -> Unit = { _, _ -> }
) {""",
"""    isExposureLocked: Boolean = false,
    onTapToFocus: (Float, Float) -> Unit = { _, _ -> },
    onLongPressToLock: (Float, Float) -> Unit = { _, _ -> }
) {""")

target = """                setOnTouchListener { view, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                        val factory = meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                        currentCamera?.cameraControl?.startFocusAndMetering(action)
                        onTapToFocus(event.x, event.y)
                        return@setOnTouchListener true
                    }
                    false
                }"""

replacement = """                val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
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
                })
                setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    true
                }"""

if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
    print("Updated CameraPreview.kt")
else:
    print("Could not find target block in CameraPreview.kt")
