import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

target = """    LaunchedEffect(tapFocusPoint) {
        if (tapFocusPoint != null) {
            focusPulseRadius.snapTo(80f)
            focusPulseAlpha.snapTo(1f)
            launch {
                focusPulseRadius.animateTo(
                    targetValue = 40f,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f)
                )
            }
            kotlinx.coroutines.delay(800)
            focusPulseAlpha.animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.tween(300)
            )
            tapFocusPoint = null
        }
    }"""

replacement = """    LaunchedEffect(tapFocusPoint, isExposureLocked) {
        if (tapFocusPoint != null) {
            focusPulseRadius.snapTo(80f)
            focusPulseAlpha.snapTo(1f)
            launch {
                focusPulseRadius.animateTo(
                    targetValue = 40f,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f)
                )
            }
            if (!isExposureLocked) {
                kotlinx.coroutines.delay(800)
                focusPulseAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(300)
                )
                tapFocusPoint = null
            }
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
    print("Replaced LaunchedEffect")
else:
    print("Could not find target LaunchedEffect")
