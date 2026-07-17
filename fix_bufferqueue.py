import re
content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()
content = content.replace("implementationMode = PreviewView.ImplementationMode.PERFORMANCE", "implementationMode = PreviewView.ImplementationMode.COMPATIBLE")
open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
