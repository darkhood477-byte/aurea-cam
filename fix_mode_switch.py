import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

target1 = """                        .clickable { isVideoMode = false }"""
replacement1 = """                        .clickable { if (!isRecording) isVideoMode = false }"""

target2 = """                        .clickable { isVideoMode = true }"""
replacement2 = """                        .clickable { if (!isRecording) isVideoMode = true }"""

content = content.replace(target1, replacement1)
content = content.replace(target2, replacement2)

# Also for camera flip
target3 = """                androidx.compose.material3.IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },"""
replacement3 = """                androidx.compose.material3.IconButton(
                    onClick = {
                        if (!isRecording) {
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        }
                    },"""
content = content.replace(target3, replacement3)

open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
print("Fixed clickable")
