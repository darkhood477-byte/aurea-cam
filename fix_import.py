import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()
if "import androidx.compose.ui.zIndex" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.zIndex")
    open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
    print("Added zIndex import")
else:
    print("zIndex import already present")
