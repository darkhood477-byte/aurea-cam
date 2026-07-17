package com.example.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    resolution: String,
    onResolutionChange: (String) -> Unit,
    frameRate: String,
    onFrameRateChange: (String) -> Unit,
    zebraStripes: Boolean,
    onZebraStripesChange: (Boolean) -> Unit,
    audioSource: String,
    onAudioSourceChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF000000))
                .padding(24.dp)
                .clickable(enabled = false) {}, // Prevent dismiss when clicking on the sheet
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            
            SettingsGroup {
                SettingRowSegmented(
                    title = "Resolution",
                    options = listOf("1080p", "4K", "720p", "480p"),
                    selectedOption = resolution,
                    onOptionSelect = onResolutionChange
                )
                
                HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 1.dp)
                
                SettingRowSegmented(
                    title = "Frame Rate",
                    options = listOf("24 fps", "30 fps", "60 fps"),
                    selectedOption = frameRate,
                    onOptionSelect = onFrameRateChange
                )
            }

            SettingsGroup {
                SettingRowSegmented(
                    title = "Audio Source",
                    options = listOf("Internal", "Bluetooth Mic", "Wired Mic", "Off"),
                    selectedOption = audioSource,
                    onOptionSelect = onAudioSourceChange
                )
            }

            SettingsGroup {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Zebra Stripes", color = Color.White, fontSize = 16.sp)
                    Switch(
                        checked = zebraStripes,
                        onCheckedChange = onZebraStripesChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = com.example.ui.theme.Orange500,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2C2C2E)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
    }
}

@Composable
fun SettingRowSegmented(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        SegmentedControl(options, selectedOption, onOptionSelect)
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C2C2E))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFF3A3A3C) else Color.Transparent)
                    .clickable { onOptionSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
