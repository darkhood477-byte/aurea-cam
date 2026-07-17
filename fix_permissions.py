import re
content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()
target = """    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted && !permissionsState.shouldShowRationale) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }"""
replacement = """    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }"""
content = content.replace(target, replacement)
open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
