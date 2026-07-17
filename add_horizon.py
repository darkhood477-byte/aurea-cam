import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

imports = """import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer"""
content = content.replace("import androidx.compose.material.icons.filled.Timer", imports)

state_target = "    var timerMode by remember { mutableStateOf(CameraTimer.OFF) }"
state_replacement = """    var timerMode by remember { mutableStateOf(CameraTimer.OFF) }
    var isVirtualHorizonEnabled by remember { mutableStateOf(false) }"""
content = content.replace(state_target, state_replacement)

overlay_target = """            // Overlays
            androidx.compose.animation.Crossfade("""
overlay_replacement = """            // Virtual Horizon
            if (isVirtualHorizonEnabled) {
                VirtualHorizon(modifier = Modifier.matchParentSize())
            }

            // Overlays
            androidx.compose.animation.Crossfade("""
content = content.replace(overlay_target, overlay_replacement)

toggle_target = """                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        timerMode = when (timerMode) {"""
toggle_replacement = """                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { isVirtualHorizonEnabled = !isVirtualHorizonEnabled }
                ) {
                    Icon(Icons.Filled.ScreenRotation, contentDescription = "Level", tint = if (isVirtualHorizonEnabled) com.example.ui.theme.Orange500 else Color.White, modifier = Modifier.size(20.dp))
                    Text("LEVEL", color = if (isVirtualHorizonEnabled) com.example.ui.theme.Orange500 else Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        timerMode = when (timerMode) {"""
content = content.replace(toggle_target, toggle_replacement)

open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
print("Updated CameraScreen.kt")
