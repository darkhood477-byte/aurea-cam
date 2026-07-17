import re
content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()

target = """            if (isVideoMode) {
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
            }"""

replacement = """            currentCamera = provider.bindToLifecycle(
                lifecycleOwner,
                resolvedSelector,
                preview,
                imageCapture,
                videoCapture
            )"""

content = content.replace(target, replacement)
open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
