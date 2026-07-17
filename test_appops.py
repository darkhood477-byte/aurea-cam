import re
content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()

content = content.replace("cameraProvider?.unbindAll()", "// cameraProvider?.unbindAll()")

open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
