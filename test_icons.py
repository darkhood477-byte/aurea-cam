import re
content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

imports = """import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Details
"""
content = content.replace("import androidx.compose.material.icons.filled.Grid3x3", "import androidx.compose.material.icons.filled.Grid3x3\n" + imports)

target = """                val overlays = listOf(
                    "THIRDS" to (CompositionOverlay.RULE_OF_THIRDS to Icons.Filled.Grid3x3),
                    "SPIRAL" to (CompositionOverlay.GOLDEN_SPIRAL to Icons.Filled.Circle),
                    "TRIANGLE" to (CompositionOverlay.GOLDEN_TRIANGLE to Icons.Filled.ChangeHistory),
                    "LINES" to (CompositionOverlay.LEADING_LINES to Icons.Filled.LinearScale)
                )"""

replacement = """                val overlays = listOf(
                    "THIRDS" to (CompositionOverlay.RULE_OF_THIRDS to Icons.Filled.Grid3x3),
                    "PHI GRID" to (CompositionOverlay.GOLDEN_RATIO to Icons.Filled.Grid4x4),
                    "SPIRAL" to (CompositionOverlay.GOLDEN_SPIRAL to Icons.Filled.Circle),
                    "TRIANGLE" to (CompositionOverlay.GOLDEN_TRIANGLE to Icons.Filled.ChangeHistory),
                    "SYMMETRY" to (CompositionOverlay.DYNAMIC_SYMMETRY to Icons.Filled.Details)
                )"""
content = content.replace(target, replacement)
open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
