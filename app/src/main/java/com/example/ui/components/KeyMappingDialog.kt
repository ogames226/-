package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GamepadConfig
import com.example.model.KeyBinding
import com.example.ui.theme.*

data class KeyPreset(
    val display: String,
    val keyLabel: String,
    val jsKey: String,
    val jsCode: String,
    val jsKeyCode: Int
)

@Composable
fun KeyMappingDialog(
    currentConfig: GamepadConfig,
    onDismiss: () -> Unit,
    onSave: (GamepadConfig) -> Unit
) {
    var buttonA by remember { mutableStateOf(currentConfig.buttonA) }
    var buttonB by remember { mutableStateOf(currentConfig.buttonB) }
    var buttonX by remember { mutableStateOf(currentConfig.buttonX) }
    var buttonY by remember { mutableStateOf(currentConfig.buttonY) }
    var buttonSpace by remember { mutableStateOf(currentConfig.buttonSpace) }
    var buttonEnter by remember { mutableStateOf(currentConfig.buttonEnter) }

    val presetOptions = listOf(
        KeyPreset("Z", "Z", "z", "KeyZ", 90),
        KeyPreset("X", "X", "x", "KeyX", 88),
        KeyPreset("C", "C", "c", "KeyC", 67),
        KeyPreset("V", "V", "v", "KeyV", 86),
        KeyPreset("A", "A", "a", "KeyA", 65),
        KeyPreset("S", "S", "s", "KeyS", 83),
        KeyPreset("D", "D", "d", "KeyD", 68),
        KeyPreset("W", "W", "w", "KeyW", 87),
        KeyPreset("Space", "SPACE", " ", "Space", 32),
        KeyPreset("Enter", "ENTER", "Enter", "Enter", 13),
        KeyPreset("Shift", "SHIFT", "Shift", "ShiftLeft", 16),
        KeyPreset("Ctrl", "CTRL", "Control", "ControlLeft", 17),
        KeyPreset("1", "1", "1", "Digit1", 49),
        KeyPreset("2", "2", "2", "Digit2", 50)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BentoSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BentoLilac,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = BentoLilacDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Gamepad Key Mapping",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Text(
                    text = "Assign Flash / ActionScript key inputs to the virtual controller buttons:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        KeyBindingRow(
                            buttonLabel = "Button A",
                            badgeColor = ButtonAColor,
                            currentBinding = buttonA,
                            presets = presetOptions,
                            onSelect = { preset ->
                                buttonA = KeyBinding(
                                    actionName = "Action A",
                                    keyLabel = preset.keyLabel,
                                    jsKey = preset.jsKey,
                                    jsCode = preset.jsCode,
                                    jsKeyCode = preset.jsKeyCode
                                )
                            }
                        )
                    }
                    item {
                        KeyBindingRow(
                            buttonLabel = "Button B",
                            badgeColor = ButtonBColor,
                            currentBinding = buttonB,
                            presets = presetOptions,
                            onSelect = { preset ->
                                buttonB = KeyBinding(
                                    actionName = "Action B",
                                    keyLabel = preset.keyLabel,
                                    jsKey = preset.jsKey,
                                    jsCode = preset.jsCode,
                                    jsKeyCode = preset.jsKeyCode
                                )
                            }
                        )
                    }
                    item {
                        KeyBindingRow(
                            buttonLabel = "Button X",
                            badgeColor = ButtonXColor,
                            currentBinding = buttonX,
                            presets = presetOptions,
                            onSelect = { preset ->
                                buttonX = KeyBinding(
                                    actionName = "Action X",
                                    keyLabel = preset.keyLabel,
                                    jsKey = preset.jsKey,
                                    jsCode = preset.jsCode,
                                    jsKeyCode = preset.jsKeyCode
                                )
                            }
                        )
                    }
                    item {
                        KeyBindingRow(
                            buttonLabel = "Button Y",
                            badgeColor = ButtonYColor,
                            currentBinding = buttonY,
                            presets = presetOptions,
                            onSelect = { preset ->
                                buttonY = KeyBinding(
                                    actionName = "Action Y",
                                    keyLabel = preset.keyLabel,
                                    jsKey = preset.jsKey,
                                    jsCode = preset.jsCode,
                                    jsKeyCode = preset.jsKeyCode
                                )
                            }
                        )
                    }
                    item {
                        KeyBindingRow(
                            buttonLabel = "SPACE (Jump/Fire)",
                            badgeColor = BentoLilac,
                            currentBinding = buttonSpace,
                            presets = presetOptions,
                            onSelect = { preset ->
                                buttonSpace = KeyBinding(
                                    actionName = "Space / Fire",
                                    keyLabel = preset.keyLabel,
                                    jsKey = preset.jsKey,
                                    jsCode = preset.jsCode,
                                    jsKeyCode = preset.jsKeyCode
                                )
                            }
                        )
                    }
                    item {
                        KeyBindingRow(
                            buttonLabel = "ENTER (Start/Select)",
                            badgeColor = BentoLilacLight,
                            currentBinding = buttonEnter,
                            presets = presetOptions,
                            onSelect = { preset ->
                                buttonEnter = KeyBinding(
                                    actionName = "Enter / Start",
                                    keyLabel = preset.keyLabel,
                                    jsKey = preset.jsKey,
                                    jsCode = preset.jsCode,
                                    jsKeyCode = preset.jsKeyCode
                                )
                            }
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val newConfig = currentConfig.copy(
                                buttonA = buttonA,
                                buttonB = buttonB,
                                buttonX = buttonX,
                                buttonY = buttonY,
                                buttonSpace = buttonSpace,
                                buttonEnter = buttonEnter
                            )
                            onSave(newConfig)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoLilac,
                            contentColor = BentoLilacDark
                        )
                    ) {
                        Text("Save Preset", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyBindingRow(
    buttonLabel: String,
    badgeColor: Color,
    currentBinding: KeyBinding,
    presets: List<KeyPreset>,
    onSelect: (KeyPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BentoSurfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    modifier = Modifier.size(14.dp)
                ) {}
                Text(
                    text = buttonLabel,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BentoSurfaceHero,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BentoLilac.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = true }
                ) {
                    Text(
                        text = "Key: ${currentBinding.keyLabel} (${currentBinding.jsKeyCode})",
                        color = BentoLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(BentoSurfaceElevated)
                ) {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text("${preset.display} (Key '${preset.jsKey}')", color = TextPrimary) },
                            onClick = {
                                onSelect(preset)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
