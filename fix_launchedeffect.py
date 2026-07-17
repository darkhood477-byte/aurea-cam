import re
content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()

content = content.replace("LaunchedEffect(cameraSelector, isVideoMode) {", "LaunchedEffect(cameraSelector) {")

open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
