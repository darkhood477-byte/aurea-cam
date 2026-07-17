import re

content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

# 1. Update CameraPreviewWithUseCases
target_preview = """            CameraPreviewWithUseCases(
                cameraSelector = cameraSelector,
                imageCapture = imageCapture,
                videoCapture = videoCapture,
                isVideoMode = isVideoMode,
                focusDistance = focusDistance,
                isExposureLocked = isExposureLocked,
                onTapToFocus = { x, y ->
                    tapFocusPoint = androidx.compose.ui.geometry.Offset(x, y)
                }
            )"""

replacement_preview = """            CameraPreviewWithUseCases(
                cameraSelector = cameraSelector,
                imageCapture = imageCapture,
                videoCapture = videoCapture,
                isVideoMode = isVideoMode,
                focusDistance = focusDistance,
                isExposureLocked = isExposureLocked,
                onTapToFocus = { x, y ->
                    isExposureLocked = false
                    tapFocusPoint = androidx.compose.ui.geometry.Offset(x, y)
                },
                onLongPressToLock = { x, y ->
                    isExposureLocked = true
                    tapFocusPoint = androidx.compose.ui.geometry.Offset(x, y)
                }
            )"""

if target_preview in content:
    content = content.replace(target_preview, replacement_preview)
    print("Replaced preview")

# 2. Remove AE LOCK from left panel
target_ae_label = """                        "FOCUS" to if (focusDistance == null) "AF" else "MF",
                        "AE LOCK" to if (isExposureLocked) "ON" else "OFF",
                        "METER" to "MATRIX\""""
replacement_ae_label = """                        "FOCUS" to if (focusDistance == null) "AF" else "MF",
                        "METER" to "MATRIX\""""

if target_ae_label in content:
    content = content.replace(target_ae_label, replacement_ae_label)
    print("Replaced ae label")

# 3. Remove AE LOCK switch
target_ae_switch = """                        // AE Lock Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.width(160.dp)
                        ) {
                            Text("AE LOCK", color = Color.White, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            androidx.compose.material3.Switch(
                                checked = isExposureLocked,
                                onCheckedChange = { isExposureLocked = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = com.example.ui.theme.Orange500, checkedTrackColor = com.example.ui.theme.Orange500.copy(alpha = 0.5f))
                            )
                        }"""
if target_ae_switch in content:
    content = content.replace(target_ae_switch, "")
    print("Replaced ae switch")

# 4. Add AE/AF LOCK Badge
target_badge_hook = """            // Stabilization active text"""
replacement_badge = """            // AE/AF LOCK Badge
            if (isExposureLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 90.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(com.example.ui.theme.Orange500)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("AE/AF LOCK", color = Color.Black, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }

            // Stabilization active text"""

if target_badge_hook in content:
    content = content.replace(target_badge_hook, replacement_badge)
    print("Added badge")

open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
