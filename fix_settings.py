import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

state_target = "    var timerMode by remember { mutableStateOf(CameraTimer.OFF) }"
state_replacement = """    var timerMode by remember { mutableStateOf(CameraTimer.OFF) }
    var isGlobalSettingsExpanded by remember { mutableStateOf(false) }
    var resolution by remember { mutableStateOf("4K") }
    var frameRate by remember { mutableStateOf("60 fps") }
    var zebraStripes by remember { mutableStateOf(false) }
    var audioSource by remember { mutableStateOf("Internal") }"""

content = content.replace(state_target, state_replacement)

# To add the SettingsSheet at the end of the Box
sheet_target = "        // Left side settings panel"
sheet_replacement = """        if (zebraStripes) {
            ZebraStripesOverlay(modifier = Modifier.matchParentSize())
        }

        // Left side settings panel"""

content = content.replace(sheet_target, sheet_replacement)

end_of_box_target = """        // Footer
        Column("""
end_of_box_replacement = """        // Global Settings Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = isGlobalSettingsExpanded,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier.zIndex(100f)
        ) {
            SettingsSheet(
                onDismiss = { isGlobalSettingsExpanded = false },
                resolution = resolution,
                onResolutionChange = { resolution = it },
                frameRate = frameRate,
                onFrameRateChange = { frameRate = it },
                zebraStripes = zebraStripes,
                onZebraStripesChange = { zebraStripes = it },
                audioSource = audioSource,
                onAudioSourceChange = { audioSource = it }
            )
        }
        
        // Footer
        Column("""

content = content.replace(end_of_box_target, end_of_box_replacement)

# Make the settings icon clickable
icon_target = """Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(24.dp))"""
icon_replacement = """Icon(
                    Icons.Filled.Settings, 
                    contentDescription = "Settings", 
                    tint = if (isGlobalSettingsExpanded) com.example.ui.theme.Orange500 else Color.White, 
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { isGlobalSettingsExpanded = !isGlobalSettingsExpanded }
                )"""

content = content.replace(icon_target, icon_replacement)

open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
print("Updated settings")
