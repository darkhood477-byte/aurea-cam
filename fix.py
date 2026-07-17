import re

content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()

target = """    DisposableEffect(Unit) {
        onDispose {
            // CameraX automatically unbinds when the lifecycle owner is destroyed.
            // Removing manual unbindAll() to prevent AppOps 'Operation not started' false-positives.
        }
    }"""
replacement = """    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
    print("Reverted to unbindAll")
else:
    print("Not found")
