import re
content = open("app/src/main/java/com/example/camera/CameraPreview.kt", "r").read()
if "import kotlin.coroutines.resume" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport kotlin.coroutines.resume")
open("app/src/main/java/com/example/camera/CameraPreview.kt", "w").write(content)
