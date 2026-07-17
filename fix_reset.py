import re
content = open("app/src/main/java/com/example/camera/CameraScreen.kt", "r").read()

target = """                        // Focus Slider
                        Column {
                            Text("FOCUS: ${if (focusDistance == null) "AUTO" else String.format(java.util.Locale.US, "%.2f", focusDistance)}", color = Color.White, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Slider(
                                value = focusDistance ?: 0f,
                                onValueChange = { focusDistance = if (it < 0.1f) null else it },
                                valueRange = 0f..10f,
                                modifier = Modifier.width(160.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = com.example.ui.theme.Orange500)
                            )
                        }
                    }"""
                    
replacement = """                        // Focus Slider
                        Column {
                            Text("FOCUS: ${if (focusDistance == null) "AUTO" else String.format(java.util.Locale.US, "%.2f", focusDistance)}", color = Color.White, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Slider(
                                value = focusDistance ?: 0f,
                                onValueChange = { focusDistance = if (it < 0.1f) null else it },
                                valueRange = 0f..10f,
                                modifier = Modifier.width(160.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = com.example.ui.theme.Orange500)
                            )
                        }

                        // Reset Button
                        Text(
                            "RESET SETTINGS",
                            color = com.example.ui.theme.Orange500,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable {
                                    isoValue = 400f
                                    shutterValue = 125f
                                    wbValue = 5600f
                                    focusDistance = null
                                }
                                .padding(8.dp)
                        )
                    }"""

content = content.replace(target, replacement)
open("app/src/main/java/com/example/camera/CameraScreen.kt", "w").write(content)
