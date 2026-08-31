package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamepadTheme
import com.example.model.RenderQuality
import com.example.ui.theme.*
import com.example.viewmodel.FlashPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FlashPlayerViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var defaultQuality by remember { mutableStateOf(RenderQuality.HIGH) }
    var gamepadTheme by remember { mutableStateOf(GamepadTheme.RETRO_ARCADE) }
    var enableHaptics by remember { mutableStateOf(true) }
    var enableHardwareAccel by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "SETTINGS",
                            color = BentoLilac,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            "Emulator Preferences",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("settings_back_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BentoSurfaceContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BentoCanvas)
            )
        },
        containerColor = BentoCanvas
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Ruffle Engine Bento Card
            BentoSettingsSectionHeader(title = "RUFFLE WASM ENGINE", icon = Icons.Default.Bolt)
            Surface(
                color = BentoSurfaceHero,
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Quality Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Render Quality", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Vector anti-aliasing level in Ruffle", color = TextSecondary, fontSize = 11.sp)
                        }
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BentoLilac,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { expanded = true }
                            ) {
                                Text(
                                    text = defaultQuality.name,
                                    color = BentoLilacDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(BentoSurfaceElevated)
                            ) {
                                RenderQuality.values().forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q.name, color = TextPrimary) },
                                        onClick = {
                                            defaultQuality = q
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BentoBorder)

                    // Hardware Acceleration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware Acceleration", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("GPU compositing for WebAssembly canvas", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = enableHardwareAccel,
                            onCheckedChange = { enableHardwareAccel = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoLilacDark,
                                checkedTrackColor = BentoLilac,
                                uncheckedTrackColor = BentoSurfaceContainer
                            )
                        )
                    }
                }
            }

            // Virtual Gamepad Bento Card
            BentoSettingsSectionHeader(title = "VIRTUAL GAMEPAD", icon = Icons.Default.SportsEsports)
            Surface(
                color = BentoSurfaceHero,
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Controller Theme
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Controller Visual Style", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GamepadTheme.values().forEach { theme ->
                                val isSelected = gamepadTheme == theme
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) BentoLilac else BentoSurfaceContainer,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) BentoLilac else BentoBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { gamepadTheme = theme }
                                ) {
                                    Text(
                                        text = when (theme) {
                                            GamepadTheme.RETRO_ARCADE -> "Arcade"
                                            GamepadTheme.NEO_CYBERPUNK -> "Cyber"
                                            GamepadTheme.MINIMAL_TRANSPARENT -> "Glass"
                                            GamepadTheme.CLASSIC_CONSOLE -> "Console"
                                        },
                                        color = if (isSelected) BentoLilacDark else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BentoBorder)

                    // Vibration Haptics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vibration & Haptics", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tactile response on gamepad & touch inputs", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = enableHaptics,
                            onCheckedChange = { enableHaptics = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoLilacDark,
                                checkedTrackColor = BentoLilac,
                                uncheckedTrackColor = BentoSurfaceContainer
                            )
                        )
                    }
                }
            }

            // About Bento Card
            BentoSettingsSectionHeader(title = "PRESERVATION & CREDITS", icon = Icons.Default.Info)
            Surface(
                color = BentoSurfaceHero,
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Flash Player Wasm Runtime",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Powered by Ruffle (Open-source Flash Player emulator written in Rust and compiled to WebAssembly). Supports ActionScript 1, 2, and 3 content, standalone .swf animations, and Windows Flash Projector .exe executables.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BentoSurfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Emulator Engine", color = TextMuted, fontSize = 11.sp)
                            Text("Ruffle Wasm • Hardware Accelerated", color = BentoLilac, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BentoSettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = BentoLilac, modifier = Modifier.size(16.dp))
        Text(
            text = title,
            color = BentoLilac,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}
