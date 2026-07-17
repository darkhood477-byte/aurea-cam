import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

target = """                isExposureLocked = isExposureLocked,
                onTapToFocus = { x, y ->
                    isExposureLocked = false
                    tapFocusPoint = androidx.compose.ui.geometry.Offset(x, y)
                },
                onLongPressToLock = { x, y ->
                    isExposureLocked = true
                    tapFocusPoint = androidx.compose.ui.geometry.Offset(x, y)
                }
            )"""

replacement = """                isExposureLocked = isExposureLocked,
                exposureCompensation = exposureCompensation,
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
                    // dy is positive when swiping up, negative when swiping down (or vice versa)
                    // Let's say drag up increases exposure. In Android, dy < 0 means finger moved up.
                    // So we subtract dy.
                    exposureCompensation = (exposureCompensation - dy * 0.05f).coerceIn(-12f, 12f)
                }
            )"""

content = content.replace(target, replacement)
open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
print("Updated CameraScreen Call")
